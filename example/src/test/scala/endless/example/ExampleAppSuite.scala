package endless.example

import cats.effect.IO
import cats.syntax.show.*
import endless.example.app.HttpServer.*
import endless.example.data.Booking.BookingID
import endless.example.data.Vehicle.VehicleID
import endless.example.data.{Booking, LatLon, Speed}
import io.circe.generic.auto.*
import munit.catseffect.IOFixture
import org.http4s.Method.*
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.client.Client
import org.http4s.client.dsl.io.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*

trait ExampleAppSuite { self: munit.CatsEffectSuite =>
  protected val client: IOFixture[Client[IO]] =
    ResourceSuiteLocalFixture("booking-client", BlazeClientBuilder[IO].resource)
  def port: Int
  private lazy val baseUri = Uri.unsafeFromString(s"http://localhost:$port")
  private val baseBookingUri = baseUri / "booking"
  private val baseVehicleUri = baseUri / "vehicle"

  test("POST booking creates booking") {
    val bookingRequest = BookingRequest(Instant.now, 1, LatLon(0, 0), LatLon(1, 1))
    for {
      bookingID <- client().expect[BookingID](POST(bookingRequest, baseBookingUri))
      _ <- IO.sleep(1.second)
      _ <- assertIO(
        client().expect[Booking](GET(baseBookingUri / bookingID.show)),
        Booking(
          bookingID,
          bookingRequest.time,
          bookingRequest.origin,
          bookingRequest.destination,
          bookingRequest.passengerCount,
          Booking.Status.Accepted
        )
      )
    } yield ()
  }

  test("PATCH booking modifies booking") {
    val bookingRequest = BookingRequest(Instant.now, 2, LatLon(0, 0), LatLon(1, 1))
    for {
      bookingID <- client().expect[BookingID](POST(bookingRequest, baseBookingUri))
      _ <- client().status(
        PATCH(BookingPatch(Some(LatLon(2, 2)), None), baseBookingUri / bookingID.show)
      )
      _ <- client().status(
        PATCH(BookingPatch(None, Some(LatLon(3, 3))), baseBookingUri / bookingID.show)
      )
      _ <- client().status(
        PATCH(BookingPatch(Some(LatLon(4, 4)), Some(LatLon(5, 5))), baseBookingUri / bookingID.show)
      )
      _ <- IO.sleep(1.second)
      _ <- assertIO(
        client().expect[Booking](GET(baseBookingUri / bookingID.show)),
        Booking(
          bookingID,
          bookingRequest.time,
          LatLon(4, 4),
          LatLon(5, 5),
          bookingRequest.passengerCount,
          Booking.Status.Accepted
        )
      )
    } yield ()
  }

  test("GET booking for unknown ID fails") {
    assertIO(
      client().status(GET(baseBookingUri / BookingID(UUID.randomUUID()).show)).map(_.code),
      400
    )
  }

  test("PATCH booking for unknown ID fails") {
    assertIO(
      client()
        .status(
          PATCH(
            BookingPatch(Some(LatLon(1, 1)), None),
            baseBookingUri / BookingID(UUID.randomUUID()).show
          )
        )
        .map(_.code),
      400
    )
  }

  test("POST bookingID/cancel cancels booking") {
    val bookingRequest = BookingRequest(Instant.now, 1, LatLon(0, 0), LatLon(1, 1))
    for {
      bookingID <- client().expect[BookingID](POST(bookingRequest, baseBookingUri))
      _ <- client().status(POST(baseBookingUri / bookingID.show / "cancel"))
      _ <- assertIO(
        client().expect[Booking](GET(baseBookingUri / bookingID.show)),
        Booking(
          bookingID,
          bookingRequest.time,
          bookingRequest.origin,
          bookingRequest.destination,
          bookingRequest.passengerCount,
          Booking.Status.Cancelled
        )
      )
    } yield ()
  }

  test("POST bookingID/cancel for unknown ID fails") {
    assertIO(
      client()
        .status(
          POST(baseBookingUri / BookingID(UUID.randomUUID()).show / "cancel")
        )
        .map(_.code),
      400
    )
  }

  test(
    "POST several positions & speeds of a vehicle and validate passivation occurs in-between calls"
  ) {
    for {
      vehicleID <- IO.pure(VehicleID(UUID.randomUUID()))
      _ <- client().status(POST(LatLon(1, 1), baseVehicleUri / vehicleID.show / "position"))
      _ <- client().status(POST(Speed(1), baseVehicleUri / vehicleID.show / "speed"))
      _ <- client().status(POST(Speed(2), baseVehicleUri / vehicleID.show / "speed"))
      _ <- client().status(POST(LatLon(2, 2), baseVehicleUri / vehicleID.show / "position"))
      _ <- client().status(POST(LatLon(3, 3), baseVehicleUri / vehicleID.show / "position"))
      _ <- client().status(POST(LatLon(4, 4), baseVehicleUri / vehicleID.show / "position"))
      _ <- client().status(POST(Speed(0), baseVehicleUri / vehicleID.show / "speed"))
      _ <- assertIO(client().expect[Int](GET(baseVehicleUri / vehicleID.show / "recoveryCount")), 1)
      _ <- IO.sleep(2.seconds) // passivation occurs
      _ <- assertIO(
        client().expect[LatLon](GET(baseVehicleUri / vehicleID.show / "position")),
        LatLon(4, 4)
      )
      _ <- assertIO(client().expect[Int](GET(baseVehicleUri / vehicleID.show / "recoveryCount")), 2)
      _ <- IO.sleep(2.seconds) // passivation occurs
      _ <- assertIO(
        client().expect[Speed](GET(baseVehicleUri / vehicleID.show / "speed")),
        Speed(0)
      )
      _ <- assertIO(client().expect[Int](GET(baseVehicleUri / vehicleID.show / "recoveryCount")), 3)
    } yield ()
  }

  test("GET vehicle position for unknown ID fails") {
    assertIO(
      client()
        .status(
          GET(baseVehicleUri / VehicleID(UUID.randomUUID()).show / "position")
        )
        .map(_.code),
      400
    )
  }

  test("GET vehicle speed for unknown ID fails") {
    assertIO(
      client()
        .status(
          GET(baseVehicleUri / VehicleID(UUID.randomUUID()).show / "speed")
        )
        .map(_.code),
      400
    )
  }

  test("GET health returns OK") {
    assertIO(client().status(GET(baseUri / "health")).map(_.code), 200)
  }
}
