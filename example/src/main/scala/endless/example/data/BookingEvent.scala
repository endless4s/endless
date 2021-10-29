package endless.example.data

import endless.example.data.Booking.{BookingID, LatLon}

sealed trait BookingEvent

object BookingEvent {
  final case class BookingPlaced(
      bookingID: BookingID,
      origin: LatLon,
      destination: LatLon,
      passengerCount: Int
  ) extends BookingEvent
  final case class OriginChanged(newOrigin: LatLon) extends BookingEvent
  final case class DestinationChanged(newDestination: LatLon) extends BookingEvent
  final object BookingCancelled extends BookingEvent
}
