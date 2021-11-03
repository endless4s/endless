package endless.example.logic

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import cats.syntax.applicative._
import endless.core.typeclass.entity.Effector
import endless.example.data.Booking
import org.typelevel.log4cats.Logger
import scala.concurrent.duration._

//#definition
object BookingEffector {
  def apply[F[_]: Logger: Monad](effector: Effector[F, Booking]): F[Unit] = {
    import effector._
    for {
      maybeBooking <- read
      _ <- maybeBooking match {
        case Some(booking) =>
          Logger[F].info(show"State is now $booking") >> (if (booking.cancelled) enablePassivation()
                                                          else enablePassivation(passivationDelay))
        case None => Logger[F].info("State is empty")
      }
    } yield ()
  }

  private val passivationDelay = 1.hour
}
//#definition
