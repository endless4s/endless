package endless.example.logic

import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import cats.{Applicative, Monad}
import endless.core.entity.{Effector, SideEffect}
import endless.example.algebra.{AvailabilityAlg, BookingAlg}
import endless.example.data.Booking
import endless.example.data.Booking.Status
import org.typelevel.log4cats.Logger

import scala.concurrent.duration._

//#definition
final case class BookingSideEffect[F[_]: Logger: Monad]()(implicit
    availabilityAlg: AvailabilityAlg[F]
) extends SideEffect[F, Booking, BookingAlg] {
  def apply(effector: Effector[F, Booking, BookingAlg]): F[Unit] = {
    import effector._

    val availabilityProcess: Booking => F[Unit] = booking =>
      booking.status match {
        case Status.Pending =>
          (availabilityAlg.isCapacityAvailable(
            booking.time,
            booking.passengerCount
          ) >>= self.notifyCapacity).void
        case _ => ().pure
      }

    val handlePassivation: Booking => F[Unit] = {
      _.status match {
        case Status.Pending   => Applicative[F].unit
        case Status.Accepted  => enablePassivation(passivationDelay)
        case Status.Rejected  => enablePassivation()
        case Status.Cancelled => enablePassivation()
      }
    }

    ifKnown(booking => Logger[F].info(show"State is now $booking")) >> ifKnown(
      availabilityProcess
    ) >> ifKnown(handlePassivation)
  }

  private val passivationDelay = 1.hour
}
//#definition
