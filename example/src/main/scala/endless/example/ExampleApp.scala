package endless.example

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import cats.effect._
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.show._
import endless.core.typeclass.entity.EntityNameProvider
import endless.core.typeclass.protocol.{EntityIDCodec, EntityIDEncoder}
import endless.example.algebra.{BookingAlg, BookingRepositoryAlg}
import endless.example.data.Booking.{BookingID, LatLon}
import endless.example.data.{Booking, BookingEvent}
import endless.example.logic.{
  BookingEffector,
  BookingEntity,
  BookingEventApplier,
  BookingRepository
}
import endless.example.protocol.BookingCommandProtocol
import endless.runtime.akka.syntax.deploy._
import io.circe.generic.auto._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.http4s.server.Server
import org.http4s.{HttpApp, HttpRoutes, Request}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID
import scala.concurrent.duration._

object ExampleApp {
  final case class BookingRequest(passengerCount: Int, origin: LatLon, destination: LatLon)
  final case class BookingPatch(origin: Option[LatLon], destination: Option[LatLon])

  // #main
  def apply(implicit actorSystem: ActorSystem[Nothing]): IO[Resource[IO, Server]] = {
    implicit val clusterSharding: ClusterSharding = ClusterSharding(actorSystem)
    implicit val commandProtocol: BookingCommandProtocol = new BookingCommandProtocol
    implicit val eventApplier: BookingEventApplier = new BookingEventApplier
    implicit val bookingEntityNameProvider: EntityNameProvider[BookingID] = () => "booking"
    implicit val idEncoder: EntityIDCodec[BookingID] =
      EntityIDCodec(_.id.toString, BookingID.fromString)
    implicit val askTimeout: Timeout = Timeout(10.seconds)

    Slf4jLogger
      .create[IO]
      .map(implicit logger => {
        deployEntity[IO, Booking, BookingEvent, BookingID, BookingAlg, BookingRepositoryAlg](
          BookingEntity(_),
          BookingRepository(_),
          BookingEffector(_)
        ).map { case (bookingRepository, _) =>
          httpService(bookingRepository)
        }
      })
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
  private def httpService(bookingRepository: BookingRepositoryAlg[IO]): HttpApp[IO] = HttpRoutes
    .of[IO] {
      case req @ POST -> Root / "booking"                => postBooking(bookingRepository, req)
      case GET -> Root / "booking" / UUIDVar(id)         => getBooking(bookingRepository, id)
      case req @ PATCH -> Root / "booking" / UUIDVar(id) => patchBooking(bookingRepository, req, id)
      case POST -> Root / "booking" / UUIDVar(id) / "cancel" => cancelBooking(bookingRepository, id)
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
}
