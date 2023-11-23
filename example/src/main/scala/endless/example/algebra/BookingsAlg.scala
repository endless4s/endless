package endless.example.algebra

import endless.example.data.Booking.BookingID

//#definition
trait BookingsAlg[F[_]] {
  def bookingFor(bookingID: BookingID): BookingAlg[F]
}
//#definition
