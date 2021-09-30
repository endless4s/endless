package endless.example.algebra

import endless.example.data.Booking.BookingID

trait BookingRepositoryAlg[F[_]] {
  def bookingFor(bookingID: BookingID): BookingAlg[F]
}
