package endless.example.app

import cats.effect.{IO, Resource}
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.show.*
import endless.example.algebra.{BookingsAlg, VehiclesAlg}
import endless.example.data.Booking.BookingID
import endless.example.data.Vehicle.VehicleID
import endless.example.data.*
import io.circe.generic.auto.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.server.Server
import org.http4s.{HttpRoutes, Request}

import java.time.Instant
import java.util.UUID

object HttpServer {
  final case class BookingRequest(
      time: Instant,
      passengerCount: Int,
      origin: LatLon,
      destination: LatLon
  )

  final case class BookingPatch(origin: Option[LatLon], destination: Option[LatLon])

  def apply(
      port: Int,
      bookingRepository: BookingsAlg[IO],
      vehicleRepository: VehiclesAlg[IO],
      isUp: IO[Boolean]
  ): Resource[IO, Server] =
    Resource
      .pure(
        // #api
        HttpRoutes
          .of[IO] {
            case req @ POST -> Root / "booking"        => postBooking(bookingRepository, req)
            case GET -> Root / "booking" / UUIDVar(id) => getBooking(bookingRepository, id)
            case req @ PATCH -> Root / "booking" / UUIDVar(id) =>
              patchBooking(bookingRepository, req, id)
            case POST -> Root / "booking" / UUIDVar(id) / "cancel" =>
              cancelBooking(bookingRepository, id)
            case GET -> Root / "vehicle" / UUIDVar(id) / "speed" =>
              getVehicleSpeed(vehicleRepository, id)
            case GET -> Root / "vehicle" / UUIDVar(id) / "position" =>
              getVehiclePosition(vehicleRepository, id)
            case GET -> Root / "vehicle" / UUIDVar(id) / "recoveryCount" =>
              getVehicleRecoveryCount(vehicleRepository, id)
            case req @ POST -> Root / "vehicle" / UUIDVar(id) / "speed" =>
              setVehicleSpeed(vehicleRepository, id, req)
            case req @ POST -> Root / "vehicle" / UUIDVar(id) / "position" =>
              setVehiclePosition(vehicleRepository, id, req)
            case GET -> Root / "health" =>
              isUp.flatMap {
                case true  => Ok("OK")
                case false => ServiceUnavailable("Cluster member is down")
              }
          }
          .orNotFound
      )
      // #api
      .flatMap(service =>
        BlazeServerBuilder[IO]
          .bindHttp(port, "localhost")
          .withHttpApp(service)
          .resource
      )

  private def postBooking(bookingRepository: BookingsAlg[IO], req: Request[IO]) =
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

  private def getBooking(bookingRepository: BookingsAlg[IO], id: UUID) =
    bookingRepository.bookingFor(BookingID(id)).get.flatMap {
      case Right(booking) => Ok(booking)
      case Left(_)        => BadRequest(show"Booking with $id doesn't exist")
    }

  private def cancelBooking(bookingRepository: BookingsAlg[IO], id: UUID) =
    bookingRepository.bookingFor(BookingID(id)).cancel.flatMap {
      case Right(_) => Ok()
      case Left(_)  => BadRequest(show"Booking with $id doesn't exist")
    }

  private def patchBooking(
      bookingRepository: BookingsAlg[IO],
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

  private def getVehicleSpeed(vehicleRepository: VehiclesAlg[IO], id: UUID) =
    vehicleRepository.vehicleFor(VehicleID(id)).getSpeed.flatMap {
      case Some(speed) => Ok(speed)
      case None        => BadRequest(show"Speed for vehicle with $id is unknown")
    }

  private def getVehiclePosition(vehicleRepository: VehiclesAlg[IO], id: UUID) =
    vehicleRepository.vehicleFor(VehicleID(id)).getPosition.flatMap {
      case Some(position) => Ok(position)
      case None           => BadRequest(show"Position for vehicle with $id is unknown")
    }

  private def getVehicleRecoveryCount(vehicleRepository: VehiclesAlg[IO], id: UUID) =
    vehicleRepository.vehicleFor(VehicleID(id)).getRecoveryCount.flatMap(count => Ok(count))

  private def setVehicleSpeed(
      vehicleRepository: VehiclesAlg[IO],
      id: UUID,
      req: Request[IO]
  ) =
    for {
      speed <- req.as[Speed]
      ok <- vehicleRepository.vehicleFor(VehicleID(id)).setSpeed(speed).flatMap(_ => Ok())
    } yield ok

  private def setVehiclePosition(
      vehicleRepository: VehiclesAlg[IO],
      id: UUID,
      req: Request[IO]
  ) =
    for {
      position <- req.as[LatLon]
      ok <- vehicleRepository.vehicleFor(VehicleID(id)).setPosition(position).flatMap(_ => Ok())
    } yield ok
}
