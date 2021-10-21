package endless.example.logic

import cats.Monad
import endless.core.typeclass.effect.Effector
import endless.core.typeclass.entity.StateReader
import endless.example.data.Booking
import org.typelevel.log4cats.Logger
import cats.syntax.flatMap._
import cats.syntax.show._

final case class BookingEffector[F[_]: Logger: Monad](reader: StateReader[F, Booking])
    extends Effector[F] {
  import reader._
  override def afterPersist: F[Unit] =
    read >>= (booking => Logger[F].info(show"State is now $booking"))
}
