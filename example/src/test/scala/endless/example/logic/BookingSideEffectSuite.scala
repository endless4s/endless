package endless.example.logic

import cats.effect.IO
import cats.syntax.either.*
import cats.syntax.show.*
import endless.\/
import endless.core.entity.Effector
import endless.core.entity.Effector.PassivationState
import endless.core.entity.SideEffect.Trigger
import endless.example.algebra.{AvailabilityAlg, BookingAlg}
import endless.example.data.{Booking, LatLon}
import org.scalacheck.effect.PropF.forAllF
import org.typelevel.log4cats.testing.TestingLogger

import java.time.Instant
import scala.concurrent.duration.*

//#example
class BookingSideEffectSuite
    extends munit.CatsEffectSuite
    with munit.ScalaCheckEffectSuite
    with Generators {
  implicit private val logger: TestingLogger[IO] = TestingLogger.impl[IO]()
  implicit private def availabilityAlg: AvailabilityAlg[IO] = (_: Instant, _: Int) => IO(true)

  test("some state generates logs after persistence") {
    forAllF { (booking: Booking) =>
      val acceptedBooking = booking.copy(status = Booking.Status.Accepted)
      for {
        effector <- Effector.apply[IO, Booking, BookingAlg](
          new SelfEntity {},
          Some(acceptedBooking)
        )
        _ <- new BookingSideEffect().apply(Trigger.AfterPersistence, effector)
        _ <- assertIO(logger.logged.map(_.map(_.message).last), show"State is now $acceptedBooking")
      } yield ()
    }
  }

  test("some state passivates after one hour") {
    forAllF { (booking: Booking, trigger: Trigger) =>
      for {
        effector <- Effector.apply[IO, Booking, BookingAlg](
          new SelfEntity {},
          Some(booking.copy(status = Booking.Status.Accepted))
        )
        _ <- new BookingSideEffect().apply(trigger, effector)
        _ <- assertIO(effector.passivationState, Effector.PassivationState.After(1.hour))
      } yield ()
    }
  }

  test("passivates immediately when cancelled") {
    forAllF { (booking: Booking, trigger: Trigger) =>
      for {
        effector <- Effector.apply[IO, Booking, BookingAlg](
          new SelfEntity {},
          Some(booking.copy(status = Booking.Status.Cancelled))
        )
        _ <- new BookingSideEffect().apply(trigger, effector)
        _ <- assertIO(effector.passivationState, PassivationState.After(Duration.Zero))
      } yield ()
    }
  }

  test("notifies availability when pending and does not passivate") {
    forAllF { (booking: Booking) =>
      for {
        effector <- Effector.apply[IO, Booking, BookingAlg](
          new SelfEntity {
            override def notifyCapacity(
                isAvailable: Boolean
            ): IO[BookingAlg.BookingUnknown.type \/ Unit] = {
              assert(isAvailable)
              IO.pure(().asRight)
            }
          },
          Some(booking.copy(status = Booking.Status.Pending))
        )
        _ <- new BookingSideEffect().apply(Trigger.AfterPersistence, effector)
      } yield ()
    }
  }

  trait SelfEntity extends BookingAlg[IO] {
    lazy val raiseError = IO.raiseError(new RuntimeException("should not be called"))

    override def place(
        bookingID: Booking.BookingID,
        time: Instant,
        passengerCount: Int,
        origin: LatLon,
        destination: LatLon
    ): IO[BookingAlg.BookingAlreadyExists \/ Unit] = raiseError
    override def get: IO[BookingAlg.BookingUnknown.type \/ Booking] = raiseError
    override def changeOrigin(newOrigin: LatLon): IO[BookingAlg.BookingUnknown.type \/ Unit] =
      raiseError
    override def changeDestination(
        newDestination: LatLon
    ): IO[BookingAlg.BookingUnknown.type \/ Unit] = raiseError
    override def changeOriginAndDestination(
        newOrigin: LatLon,
        newDestination: LatLon
    ): IO[BookingAlg.BookingUnknown.type \/ Unit] = raiseError
    override def cancel: IO[BookingAlg.CancelError \/ Unit] = raiseError
    override def notifyCapacity(isAvailable: Boolean): IO[BookingAlg.BookingUnknown.type \/ Unit] =
      raiseError
  }

}
//#example
