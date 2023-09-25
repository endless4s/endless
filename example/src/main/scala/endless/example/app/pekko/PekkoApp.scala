package endless.example.app.pekko

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.persistence.testkit.{
  PersistenceTestKitDurableStateStorePlugin,
  PersistenceTestKitPlugin
}
import org.apache.pekko.util.Timeout
import cats.effect._
import cats.syntax.applicative._
import com.typesafe.config.ConfigFactory
import endless.example.algebra._
import endless.example.app.HttpServer
import endless.example.app.impl.{Availabilities, Bookings, Vehicles}
import endless.example.data.Booking.BookingID
import endless.example.data.Vehicle.VehicleID
import endless.example.data._
import endless.example.logic._
import endless.runtime.pekko.syntax.deploy._
import endless.runtime.pekko.deploy.PekkoCluster
import endless.runtime.pekko.deploy.PekkoDeployer.PekkoDeploymentParameters
import endless.runtime.pekko.deploy.PekkoDurableDeployer.PekkoDurableDeploymentParameters
import org.apache.pekko.persistence.typed.{EventAdapter, EventSeq, SnapshotAdapter}
import org.http4s.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PekkoApp extends Bookings with Vehicles with Availabilities {

  def apply(port: Int): Resource[IO, Server] = IO.executionContext.toResource
    .flatMap(actorSystem)
    .flatMap(createPekkoApp(port))

  private def actorSystem(executionContext: ExecutionContext): Resource[IO, ActorSystem[Nothing]] =
    Resource.make(
      IO(
        ActorSystem.wrap(
          org.apache.pekko.actor.ActorSystem(
            name = "example-pekko-as",
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

  private def createPekkoApp(port: Int)(actorSystem: ActorSystem[Nothing]): Resource[IO, Server] = {
    implicit val askTimeout: Timeout = Timeout(10.seconds)

    // #main
    Resource
      .eval(Slf4jLogger.create[IO])
      .flatMap { implicit logger: Logger[IO] =>
        PekkoCluster.managedResource[IO](actorSystem).flatMap {
          implicit cluster: PekkoCluster[IO] =>
            implicit val eventSourcingDeploymentParameters
                : PekkoDeploymentParameters[IO, Booking, BookingEvent] =
              PekkoDeploymentParameters[IO, Booking, BookingEvent](
                customizeBehavior = (_, behavior) =>
                  behavior.eventAdapter(
                    new EventAdapter[
                      BookingEvent,
                      endless.example.proto.booking.events.BookingEvent
                    ] {
                      def toJournal(
                          e: BookingEvent
                      ): endless.example.proto.booking.events.BookingEvent =
                        eventAdapter.toJournal(e)

                      def manifest(event: BookingEvent): String = event.getClass.getName

                      def fromJournal(
                          p: endless.example.proto.booking.events.BookingEvent,
                          manifest: String
                      ): EventSeq[BookingEvent] = EventSeq.single(eventAdapter.fromJournal(p))
                    }
                  )
              )
            implicit val durableDeploymentParameters
                : PekkoDurableDeploymentParameters[IO, Vehicle] =
              PekkoDurableDeploymentParameters[IO, Vehicle](
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
                  BookingRepositoryAlg
                ](
                  BookingRepository(_).pure[IO],
                  BookingEntity(_).pure[IO],
                  { case (effector, _, _) => BookingEffector(effector).pure[IO] }
                ),
                deployDurableRepository[IO, VehicleID, Vehicle, VehicleAlg, VehicleRepositoryAlg](
                  VehicleRepository(_).pure[IO],
                  VehicleEntity(_).pure[IO],
                  { case (effector, _, _) => VehicleEffector.apply[IO](effector).map(_.apply) }
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
    // #main
  }
}
