package endless.example.logic

import cats.Monad
import endless.core.entity.Repository
import endless.example.algebra.{BookingAlg, BookingRepositoryAlg}
import endless.example.data.Booking.BookingID

//#definition
final case class BookingRepository[F[_]: Monad](repository: Repository[F, BookingID, BookingAlg])
    extends BookingRepositoryAlg[F] {
  def bookingFor(bookingID: BookingID): BookingAlg[F] = repository.entityFor(bookingID)
}
//#definition
