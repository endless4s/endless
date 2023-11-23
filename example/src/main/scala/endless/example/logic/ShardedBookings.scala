package endless.example.logic

import cats.Monad
import endless.core.entity.Sharding
import endless.example.algebra.{BookingAlg, BookingsAlg}
import endless.example.data.Booking.BookingID

//#definition
final case class ShardedBookings[F[_]: Monad](sharding: Sharding[F, BookingID, BookingAlg])
    extends BookingsAlg[F] {
  def bookingFor(bookingID: BookingID): BookingAlg[F] = sharding.entityFor(bookingID)
}
//#definition
