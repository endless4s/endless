package endless.example.app.akka

import akka.actor.typed.ActorSystem
import akka.persistence.testkit.{
  PersistenceTestKitDurableStateStorePlugin,
  PersistenceTestKitPlugin
}
import akka.persistence.typed.{EventAdapter, EventSeq, SnapshotAdapter}
import akka.util.Timeout
import cats.effect.*
import com.typesafe.config.ConfigFactory
import endless.core.interpret.{
  DurableBehaviorInterpreter,
  SideEffectInterpreter,
  BehaviorInterpreter,
  RepositoryInterpreter
}
import endless.example.algebra.*
import endless.example.app.HttpServer
import endless.example.app.impl.{Availabilities, Bookings, Vehicles}
import endless.example.data.Booking.BookingID
import endless.example.data.Vehicle.VehicleID
import endless.example.data.*
import endless.example.logic.*
import endless.runtime.akka.deploy.AkkaCluster
import endless.runtime.akka.deploy.AkkaDeployer.AkkaDeploymentParameters
import endless.runtime.akka.deploy.AkkaDurableDeployer.AkkaDurableDeploymentParameters
import endless.runtime.akka.syntax.deploy.*
import org.http4s.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

object AkkaApp extends Bookings with Vehicles with Availabilities {

  def apply(port: Int): Resource[IO, Server] = IO.executionContext.toResource
    .flatMap(actorSystem)
    .flatMap(createAkkaApp(port))

  private def actorSystem(executionContext: ExecutionContext): Resource[IO, ActorSystem[Nothing]] =
    Resource.make(
      IO(
        ActorSystem.wrap(
          akka.actor.ActorSystem(
            name = "example-akka-as",
            config = Some(
              PersistenceTestKitPlugin.config
                .withFallback(PersistenceTestKitDurableStateStorePlugin.config)
                .withFallback(ConfigFactory.defaultApplication)
                .resolve()
            ),
            defaultExecutionContext = Some(executionContext),
            classLoader = None
          )
        )
      )
    )(system =>
      IO.fromFuture(IO.blocking {
        system.terminate()
        system.whenTerminated
      }).void
    )

  // #main
  private def createAkkaApp(port: Int)(actorSystem: ActorSystem[Nothing]): Resource[IO, Server] = {
    implicit val askTimeout: Timeout = Timeout(10.seconds)

    Resource
      .eval(Slf4jLogger.create[IO])
      .flatMap { implicit logger: Logger[IO] =>
        AkkaCluster.managedResource[IO](actorSystem).flatMap { implicit cluster: AkkaCluster[IO] =>
          implicit val eventSourcingDeploymentParameters
              : AkkaDeploymentParameters[IO, Booking, BookingEvent] =
            AkkaDeploymentParameters[IO, Booking, BookingEvent](
              customizeBehavior = (_, behavior) =>
                behavior.eventAdapter(
                  new EventAdapter[
                    BookingEvent,
                    endless.example.proto.booking.events.BookingEvent
                  ] {
                    def toJournal(
                        event: BookingEvent
                    ): endless.example.proto.booking.events.BookingEvent =
                      eventAdapter.toJournal(event)
                    def manifest(event: BookingEvent): String = event.getClass.getName
                    def fromJournal(
                        event: endless.example.proto.booking.events.BookingEvent,
                        manifest: String
                    ): EventSeq[BookingEvent] = EventSeq.single(eventAdapter.fromJournal(event))
                  }
                )
            )
          implicit val durableDeploymentParameters: AkkaDurableDeploymentParameters[IO, Vehicle] =
            AkkaDurableDeploymentParameters[IO, Vehicle](
              customizeBehavior = (_, behavior) =>
                behavior.snapshotAdapter(new SnapshotAdapter[Option[Vehicle]] {
                  def toJournal(state: Option[Vehicle]): Any = stateAdapter.toJournal(state)
                  def fromJournal(from: Any): Option[Vehicle] = stateAdapter.fromJournal(from)
                })
            )
          Resource
            .both(
              deployRepository[
                IO,
                BookingID,
                Booking,
                BookingEvent,
                BookingAlg,
                BookingsAlg
              ](
                RepositoryInterpreter.lift(ShardedBookings(_)),
                BehaviorInterpreter.lift(BookingEntity(_)),
                SideEffectInterpreter.lift((_, _) => new BookingSideEffect())
              ),
              deployDurableRepository[IO, VehicleID, Vehicle, VehicleAlg, VehiclesAlg](
                RepositoryInterpreter.lift(ShardedVehicles(_)),
                DurableBehaviorInterpreter.lift(VehicleEntity(_)),
                (_, _) => VehicleSideEffect()
              )
            )
            .flatMap { case (bookingDeployment, vehicleDeployment) =>
              HttpServer(
                port,
                bookingDeployment.repository,
                vehicleDeployment.repository,
                cluster.isMemberUp
              )
            }
        }
      }
  }
  // #main
}
