package endless.example

import akka.actor.typed.ActorSystem
import akka.persistence.testkit.PersistenceTestKitPlugin
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.applicative._
import cats.syntax.show._
import com.typesafe.config.ConfigFactory
import endless.example.ExampleApp._
import endless.example.data.Booking
import endless.example.data.Booking.{BookingID, LatLon}
import io.circe.generic.auto._
import org.http4s.Method._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import java.time.Instant
import java.util.UUID

class ExampleAppSuite extends munit.CatsEffectSuite {
  implicit val actorSystem: ActorSystem[Nothing] =
    ActorSystem.wrap(
      akka.actor.ActorSystem(
        "bookings-as",
        PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication).resolve()
      )
    )

  private val server =
    ResourceSuiteLocalFixture(
      "booking-server",
      ExampleApp.apply
        .unsafeRunSync()
        .flatMap(server => Resource.make(server.pure[IO])(_ => IO.delay(actorSystem.terminate())))
    )
  private val client = ResourceSuiteLocalFixture("booking-client", BlazeClientBuilder[IO].resource)
  private val baseUri = uri"http://localhost:8080/booking"

  override def munitFixtures = List(server, client)

  test("post booking creates booking") {
    val bookingRequest = BookingRequest(Instant.now, 1, LatLon(0, 0), LatLon(1, 1))
    for {
      bookingID <- client().expect[BookingID](POST(bookingRequest, baseUri))
    } yield assertIO(
      client().expect[Booking](GET(baseUri / bookingID.show)),
      Booking(
        bookingID,
        bookingRequest.time,
        bookingRequest.origin,
        bookingRequest.destination,
        bookingRequest.passengerCount,
        Booking.Status.Accepted
      )
    )
  }

  test("patch booking modifies booking") {
    val bookingRequest = BookingRequest(Instant.now, 2, LatLon(0, 0), LatLon(1, 1))
    for {
      bookingID <- client().expect[BookingID](POST(bookingRequest, baseUri))
      _ <- client().status(
        PATCH(BookingPatch(Some(LatLon(2, 2)), None), baseUri / bookingID.show)
      )
      _ <- client().status(
        PATCH(BookingPatch(None, Some(LatLon(3, 3))), baseUri / bookingID.show)
      )
      _ <- client().status(
        PATCH(BookingPatch(Some(LatLon(4, 4)), Some(LatLon(5, 5))), baseUri / bookingID.show)
      )
    } yield assertIO(
      client().expect[Booking](GET(baseUri / bookingID.show)),
      Booking(
        bookingID,
        bookingRequest.time,
        LatLon(4, 4),
        LatLon(5, 5),
        bookingRequest.passengerCount,
        Booking.Status.Accepted
      )
    )
  }

  test("GET booking for unknown ID fails") {
    assertIO(client().status(GET(baseUri / BookingID(UUID.randomUUID()).show)).map(_.code), 400)
  }

  test("PATCH booking for unknown ID fails") {
    assertIO(
      client()
        .status(
          PATCH(BookingPatch(Some(LatLon(1, 1)), None), baseUri / BookingID(UUID.randomUUID()).show)
        )
        .map(_.code),
      400
    )
  }

  test("POST bookingID/cancel cancels booking") {
    val bookingRequest = BookingRequest(Instant.now, 1, LatLon(0, 0), LatLon(1, 1))
    for {
      bookingID <- client().expect[BookingID](POST(bookingRequest, baseUri))
      _ <- client().status(POST(baseUri / bookingID.show / "cancel"))
    } yield assertIO(
      client().expect[Booking](GET(baseUri / bookingID.show)),
      Booking(
        bookingID,
        bookingRequest.time,
        bookingRequest.origin,
        bookingRequest.destination,
        bookingRequest.passengerCount,
        Booking.Status.Cancelled
      )
    )
  }

  test("POST bookingID/cancel for unknown ID fails") {
    assertIO(
      client()
        .status(
          POST(baseUri / BookingID(UUID.randomUUID()).show / "cancel")
        )
        .map(_.code),
      400
    )
  }
}
