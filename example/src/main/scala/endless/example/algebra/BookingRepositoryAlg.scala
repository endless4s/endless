package endless.example.algebra

import endless.example.data.Booking.BookingID

//#definition
trait BookingRepositoryAlg[F[_]] {
  def bookingFor(bookingID: BookingID): BookingAlg[F]
}
//#definition
