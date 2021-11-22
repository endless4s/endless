package endless.example.logic

import cats.{Applicative, Monad}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import cats.syntax.applicative._
import endless.core.typeclass.entity.Effector
import endless.example.algebra.{AvailabilityAlg, BookingAlg}
import endless.example.data.Booking
import endless.example.data.Booking.Status
import org.typelevel.log4cats.Logger

import scala.concurrent.duration._

//#definition
object BookingEffector {
  def apply[F[_]: Logger: Monad](
      effector: Effector[F, Booking, BookingAlg]
  )(implicit availabilityAlg: AvailabilityAlg[F]): F[Unit] = {
    import effector._

    val availabilityProcess: Booking => F[Unit] = booking =>
      booking.status match {
        case Status.Pending =>
          for {
            isAvailable <- availabilityAlg.isCapacityAvailable(booking.time, booking.passengerCount)
            entity <- self
            _ <- entity.notifyCapacity(isAvailable)
          } yield ()
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
