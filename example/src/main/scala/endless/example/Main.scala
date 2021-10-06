package endless.example

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.util.Timeout
import cats.effect._
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.show._
import com.typesafe.config.ConfigFactory
import endless.core.interpret.EntityT._
import endless.core.typeclass.entity.EntityNameProvider
import endless.core.typeclass.protocol.EntityIDEncoder
import endless.example.algebra.{BookingAlg, BookingRepositoryAlg}
import endless.example.data.Booking.{BookingID, LatLon}
import endless.example.data.{Booking, BookingEvent}
import endless.example.logic.{BookingEntity, BookingEventApplier, BookingRepository}
import endless.example.protocol.BookingCommandProtocol
import endless.runtime.akka.syntax.deploy._
import io.circe.generic.auto._
import org.http4s.{HttpRoutes, Request}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID
import scala.concurrent.duration._

object Main extends IOApp {
  private final case class BookingRequest(passengerCount: Int, origin: LatLon, destination: LatLon)
  private final case class BookingPatch(origin: Option[LatLon], destination: Option[LatLon])

  private def httpService(bookingRepository: BookingRepositoryAlg[IO]) = HttpRoutes
    .of[IO] {
      case req @ POST -> Root / "booking"                => postBooking(bookingRepository, req)
      case GET -> Root / "booking" / UUIDVar(id)         => getBooking(bookingRepository, id)
      case req @ PATCH -> Root / "booking" / UUIDVar(id) => patchBooking(bookingRepository, req, id)
    }
    .orNotFound

  private def postBooking(bookingRepository: BookingRepositoryAlg[IO], req: Request[IO]) =
    for {
      bookingRequest <- req.as[BookingRequest]
      bookingID <- IO(UUID.randomUUID()).map(BookingID)
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

  def run(args: List[String]): IO[ExitCode] = {
    implicit val actorSystem: ActorSystem[Nothing] =
      ActorSystem.wrap(
        akka.actor.ActorSystem(
          "bookings-as",
          PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication).resolve()
        )
      )
    implicit val clusterSharding: ClusterSharding = ClusterSharding(actorSystem)
    implicit val commandProtocol: BookingCommandProtocol = new BookingCommandProtocol
    implicit val eventApplier: BookingEventApplier = new BookingEventApplier
    implicit val bookingEntityNameProvider: EntityNameProvider[BookingID] = () => "booking"
    implicit val idEncoder: EntityIDEncoder[BookingID] = _.id.toString
    implicit val askTimeout: Timeout = Timeout(10.seconds)

    Slf4jLogger
      .create[IO]
      .flatMap(implicit logger =>
        deployEntity[IO, Option[
          Booking
        ], BookingEvent, BookingID, BookingAlg, BookingRepositoryAlg](
          BookingEntity(_),
          BookingRepository(_),
          Option.empty[Booking]
        ).map { case (bookingRepository, _) =>
          httpService(bookingRepository)
        }.flatMap(service =>
          BlazeServerBuilder[IO]
            .bindHttp(8080, "localhost")
            .withHttpApp(service)
            .resource
        ).use(_ => IO.fromFuture(IO(actorSystem.whenTerminated)))
      )
      .as(ExitCode.Success)
  }

}
