package endless.example

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import cats.Monad
import cats.effect._
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import endless.core.entity.EntityNameProvider
import endless.core.interpret.EffectorT
import endless.core.protocol.EntityIDCodec
import endless.example.adapter.VehicleStateAdapter
import endless.example.algebra._
import endless.example.data.Booking.BookingID
import endless.example.data.Vehicle.VehicleID
import endless.example.data._
import endless.example.logic._
import endless.example.protocol.{BookingCommandProtocol, VehicleCommandProtocol}
import endless.runtime.akka.syntax.deploy._
import io.circe.generic.auto._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.http4s.server.Server
import org.http4s.{HttpApp, HttpRoutes, Request}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._

object ExampleApp {
  final case class BookingRequest(
      time: Instant,
      passengerCount: Int,
      origin: LatLon,
      destination: LatLon
  )
  final case class BookingPatch(origin: Option[LatLon], destination: Option[LatLon])

  // #main
  def apply(implicit actorSystem: ActorSystem[Nothing]): IO[Resource[IO, Server]] = {
    implicit val clusterSharding: ClusterSharding = ClusterSharding(actorSystem)
    implicit val bookingCommandProtocol: BookingCommandProtocol = new BookingCommandProtocol
    implicit val vehicleCommandProtocol: VehicleCommandProtocol = new VehicleCommandProtocol
    implicit val eventApplier: BookingEventApplier = new BookingEventApplier
    implicit val bookingEntityNameProvider: EntityNameProvider[BookingID] = () => "booking"
    implicit val vehicleEntityNameProvider: EntityNameProvider[VehicleID] = () => "vehicle"
    implicit val bookingIDEncoder: EntityIDCodec[BookingID] =
      EntityIDCodec(_.id.show, BookingID.fromString)
    implicit val vehicleIDEncoder: EntityIDCodec[VehicleID] =
      EntityIDCodec(_.id.show, VehicleID.fromString)
    implicit val askTimeout: Timeout = Timeout(10.seconds)

    Slf4jLogger
      .create[IO]
      .map { implicit logger: Logger[IO] =>
        Resource
          .both(
            deployEntity[IO, Booking, BookingEvent, BookingID, BookingAlg, BookingRepositoryAlg](
              BookingEntity(_),
              BookingRepository(_),
              { case (effector, _, _) => BookingEffector(effector) }
            ),
            deployDurableEntityF[IO, Vehicle, VehicleID, VehicleAlg, VehicleRepositoryAlg](
              VehicleEntity(_).pure[IO],
              VehicleRepository(_).pure[IO],
              { case (effector, _, _) => VehicleEffector.apply[IO](effector).map(_.apply) },
              customizeBehavior = (_, behavior) => behavior.snapshotAdapter(new VehicleStateAdapter)
            )
          )
          .map { case ((bookingRepository, _), (vehicleRepository, _)) =>
            httpService(bookingRepository, vehicleRepository)
          }
      }
      .map(
        _.flatMap(service =>
          BlazeServerBuilder[IO]
            .bindHttp(8080, "localhost")
            .withHttpApp(service)
            .resource
        )
      )
  }
  // #main

  // #api
  private def httpService(
      bookingRepository: BookingRepositoryAlg[IO],
      vehicleRepository: VehicleRepositoryAlg[IO]
  ): HttpApp[IO] = HttpRoutes
    .of[IO] {
      case req @ POST -> Root / "booking"                => postBooking(bookingRepository, req)
      case GET -> Root / "booking" / UUIDVar(id)         => getBooking(bookingRepository, id)
      case req @ PATCH -> Root / "booking" / UUIDVar(id) => patchBooking(bookingRepository, req, id)
      case POST -> Root / "booking" / UUIDVar(id) / "cancel" => cancelBooking(bookingRepository, id)
      case GET -> Root / "vehicle" / UUIDVar(id) / "speed" => getVehicleSpeed(vehicleRepository, id)
      case GET -> Root / "vehicle" / UUIDVar(id) / "position" =>
        getVehiclePosition(vehicleRepository, id)
      case GET -> Root / "vehicle" / UUIDVar(id) / "recoveryCount" =>
        getVehicleRecoveryCount(vehicleRepository, id)
      case req @ POST -> Root / "vehicle" / UUIDVar(id) / "speed" =>
        setVehicleSpeed(vehicleRepository, id, req)
      case req @ POST -> Root / "vehicle" / UUIDVar(id) / "position" =>
        setVehiclePosition(vehicleRepository, id, req)
    }
    .orNotFound
  // #api

  private def postBooking(bookingRepository: BookingRepositoryAlg[IO], req: Request[IO]) =
    for {
      bookingRequest <- req.as[BookingRequest]
      bookingID <- IO(UUID.randomUUID()).map(BookingID(_))
      reply <- bookingRepository
        .bookingFor(bookingID)
        .place(
          bookingID,
          bookingRequest.time,
          bookingRequest.passengerCount,
          bookingRequest.origin,
          bookingRequest.destination
        )
      result <- reply match {
        case Left(alreadyExists) =>
          BadRequest(show"Booking with ${alreadyExists.bookingID.id} already exists")
        case Right(_) => Accepted(bookingID)
      }
    } yield result

  private def getBooking(bookingRepository: BookingRepositoryAlg[IO], id: UUID) =
    bookingRepository.bookingFor(BookingID(id)).get.flatMap {
      case Right(booking) => Ok(booking)
      case Left(_)        => BadRequest(show"Booking with $id doesn't exist")
    }

  private def cancelBooking(bookingRepository: BookingRepositoryAlg[IO], id: UUID) =
    bookingRepository.bookingFor(BookingID(id)).cancel.flatMap {
      case Right(_) => Ok()
      case Left(_)  => BadRequest(show"Booking with $id doesn't exist")
    }

  private def patchBooking(
      bookingRepository: BookingRepositoryAlg[IO],
      req: Request[IO],
      id: UUID
  ) =
    for {
      bookingPatch <- req.as[BookingPatch]
      bookingID = BookingID(id)
      reply <- (bookingPatch.origin, bookingPatch.destination) match {
        case (Some(newOrigin), Some(newDestination)) =>
          bookingRepository
            .bookingFor(bookingID)
            .changeOriginAndDestination(newOrigin, newDestination)
        case (Some(newOrigin), None) =>
          bookingRepository
            .bookingFor(bookingID)
            .changeOrigin(newOrigin)
        case (None, Some(newDestination)) =>
          bookingRepository.bookingFor(bookingID).changeDestination(newDestination)
        case (None, None) => ().asRight.pure[IO]
      }
      result <- reply match {
        case Left(_) =>
          BadRequest(show"Booking with $id doesn't exist")
        case Right(_) => Ok()
      }
    } yield result

  private def getVehicleSpeed(vehicleRepository: VehicleRepositoryAlg[IO], id: UUID) =
    vehicleRepository.vehicleFor(VehicleID(id)).getSpeed.flatMap {
      case Some(speed) => Ok(speed)
      case None        => BadRequest(show"Speed for vehicle with $id is unknown")
    }

  private def getVehiclePosition(vehicleRepository: VehicleRepositoryAlg[IO], id: UUID) =
    vehicleRepository.vehicleFor(VehicleID(id)).getPosition.flatMap {
      case Some(position) => Ok(position)
      case None           => BadRequest(show"Position for vehicle with $id is unknown")
    }

  private def getVehicleRecoveryCount(vehicleRepository: VehicleRepositoryAlg[IO], id: UUID) =
    vehicleRepository.vehicleFor(VehicleID(id)).getRecoveryCount.flatMap(count => Ok(count))

  private def setVehicleSpeed(
      vehicleRepository: VehicleRepositoryAlg[IO],
      id: UUID,
      req: Request[IO]
  ) =
    for {
      speed <- req.as[Speed]
      ok <- vehicleRepository.vehicleFor(VehicleID(id)).setSpeed(speed).flatMap(_ => Ok())
    } yield ok

  private def setVehiclePosition(
      vehicleRepository: VehicleRepositoryAlg[IO],
      id: UUID,
      req: Request[IO]
  ) =
    for {
      position <- req.as[LatLon]
      ok <- vehicleRepository.vehicleFor(VehicleID(id)).setPosition(position).flatMap(_ => Ok())
    } yield ok

  implicit def alwaysAvailable[F[_]: Logger: Monad: Async]: AvailabilityAlg[F] =
    (time: Instant, passengerCount: Int) =>
      Logger[F].info(
        show"Checking if capacity is available for ${time.toString} and $passengerCount passengers"
      ) >> Async[F].sleep(500.millis) >> Logger[F].info(
        show"Availability confirmed for ${time.toString} and $passengerCount passengers"
      ) >> true
        .pure[F]
}
