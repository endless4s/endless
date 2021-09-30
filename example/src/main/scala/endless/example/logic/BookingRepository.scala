package endless.example.logic

import cats.Monad
import endless.core.typeclass.entity.Repository
import endless.example.algebra.{BookingAlg, BookingRepositoryAlg}
import endless.example.data.Booking.BookingID

final case class BookingRepository[F[_]: Monad](repository: Repository[F, BookingID, BookingAlg])
    extends BookingRepositoryAlg[F] {
  import repository._
  def bookingFor(bookingID: BookingID): BookingAlg[F] = entityFor(bookingID)
}
