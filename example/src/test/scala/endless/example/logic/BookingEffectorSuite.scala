package endless.example.logic

import cats.Applicative
import cats.effect.IO
import cats.syntax.either._
import cats.syntax.show._
import endless.\/
import endless.core.interpret.EffectorT
import endless.core.interpret.EffectorT._
import endless.example.algebra.{AvailabilityAlg, BookingAlg}
import endless.example.data.Booking
import org.scalacheck.effect.PropF._
import org.typelevel.log4cats.testing.TestingLogger

import java.time.Instant
import scala.concurrent.duration._

//#example
class BookingEffectorSuite
    extends munit.CatsEffectSuite
    with munit.ScalaCheckEffectSuite
    with Generators {
  implicit private val logger: TestingLogger[IO] = TestingLogger.impl[IO]()
  implicit private def availabilityAlg[F[_]: Applicative]: AvailabilityAlg[F] =
    (_: Instant, _: Int) => Applicative[F].pure(true)
  private val effector = BookingEffector(EffectorT.instance[IO, Booking, BookingAlg])

  test("some state log") {
    forAllF { booking: Booking =>
      val acceptedBooking = booking.copy(status = Booking.Status.Accepted)
      effector
        .runA(Some(acceptedBooking), new SelfEntity {})
        .flatMap(_ =>
          assertIO(logger.logged.map(_.map(_.message).last), show"State is now $acceptedBooking")
        )
    }
  }

  test("some state passivate after one hour") {
    forAllF { booking: Booking =>
      assertIO(
        effector.runS(Some(booking.copy(status = Booking.Status.Accepted)), new SelfEntity {}),
        PassivationState.After(1.hour)
      )
    }
  }

  test("passivate immediately when cancelled") {
    forAllF { booking: Booking =>
      assertIO(
        effector.runS(Some(booking.copy(status = Booking.Status.Cancelled)), new SelfEntity {}),
        PassivationState.After(Duration.Zero)
      )
    }
  }

  test("notifies availability when pending and does not passivate") {
    forAllF { booking: Booking =>
      assertIO(
        effector.runS(
          Some(booking.copy(status = Booking.Status.Pending)),
          new SelfEntity {
            override def notifyCapacity(
                isAvailable: Boolean
            ): IO[BookingAlg.BookingUnknown.type \/ Unit] = {
              assert(isAvailable)
              IO.pure(().asRight)
            }
          }
        ),
        PassivationState.Unchanged
      )
    }
  }

  trait SelfEntity extends BookingAlg[IO] {
    override def place(
        bookingID: Booking.BookingID,
        time: Instant,
        passengerCount: Int,
        origin: Booking.LatLon,
        destination: Booking.LatLon
    ): IO[BookingAlg.BookingAlreadyExists \/ Unit] =
      IO.raiseError(new RuntimeException("should not be called"))

    override def get: IO[BookingAlg.BookingUnknown.type \/ Booking] =
      IO.raiseError(new RuntimeException("should not be called"))

    override def changeOrigin(
        newOrigin: Booking.LatLon
    ): IO[BookingAlg.BookingUnknown.type \/ Unit] =
      IO.raiseError(new RuntimeException("should not be called"))

    override def changeDestination(
        newDestination: Booking.LatLon
    ): IO[BookingAlg.BookingUnknown.type \/ Unit] =
      IO.raiseError(new RuntimeException("should not be called"))

    override def changeOriginAndDestination(
        newOrigin: Booking.LatLon,
        newDestination: Booking.LatLon
    ): IO[BookingAlg.BookingUnknown.type \/ Unit] =
      IO.raiseError(new RuntimeException("should not be called"))

    override def cancel: IO[BookingAlg.CancelError \/ Unit] =
      IO.raiseError(new RuntimeException("should not be called"))

    override def notifyCapacity(isAvailable: Boolean): IO[BookingAlg.BookingUnknown.type \/ Unit] =
      IO.raiseError(new RuntimeException("should not be called"))
  }

}
//#example
