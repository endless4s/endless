package endless.example.data

import endless.example.data.Booking.BookingID

import java.time.Instant

sealed trait BookingEvent

object BookingEvent {
  final case class BookingPlaced(
      bookingID: BookingID,
      time: Instant,
      origin: LatLon,
      destination: LatLon,
      passengerCount: Int
  ) extends BookingEvent
  final case class OriginChanged(newOrigin: LatLon) extends BookingEvent
  final case class DestinationChanged(newDestination: LatLon) extends BookingEvent
  final object BookingAccepted extends BookingEvent
  final object BookingRejected extends BookingEvent
  final object BookingCancelled extends BookingEvent
}
