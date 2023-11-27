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
  object BookingAccepted extends BookingEvent
  object BookingRejected extends BookingEvent
  object BookingCancelled extends BookingEvent
}
