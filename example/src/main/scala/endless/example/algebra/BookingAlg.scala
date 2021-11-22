package endless.example.algebra

import endless.\/
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown, CancelError}
import endless.example.data.Booking
import endless.example.data.Booking.{BookingID, LatLon}
import cats.tagless.{Derive, FunctorK}

import java.time.Instant

//#definition
trait BookingAlg[F[_]] {
  def place(
      bookingID: BookingID,
      time: Instant,
      passengerCount: Int,
      origin: LatLon,
      destination: LatLon
  ): F[BookingAlreadyExists \/ Unit]
  def get: F[BookingUnknown.type \/ Booking]
  def changeOrigin(newOrigin: LatLon): F[BookingUnknown.type \/ Unit]
  def changeDestination(newDestination: LatLon): F[BookingUnknown.type \/ Unit]
  def changeOriginAndDestination(
      newOrigin: LatLon,
      newDestination: LatLon
  ): F[BookingUnknown.type \/ Unit]
  def cancel: F[CancelError \/ Unit]
  def notifyCapacity(isAvailable: Boolean): F[BookingUnknown.type \/ Unit]
}
//#definition

object BookingAlg {
  final case class BookingAlreadyExists(bookingID: BookingID)
  case object BookingUnknown extends CancelError

  sealed trait CancelError
  final case class BookingWasRejected(bookingID: BookingID) extends CancelError

  implicit lazy val functorKInstance: FunctorK[BookingAlg] = Derive.functorK[BookingAlg]
}
