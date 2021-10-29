package endless.example.protocol

import endless.example.data.Booking.{LatLon, BookingID}

sealed trait BookingCommand

object BookingCommand {
  final case class PlaceBooking(
      bookingID: BookingID,
      passengerCount: Int,
      origin: LatLon,
      destination: LatLon
  ) extends BookingCommand
  final case object Get extends BookingCommand
  final case class ChangeOrigin(newOrigin: LatLon) extends BookingCommand
  final case class ChangeDestination(newDestination: LatLon) extends BookingCommand
  final case class ChangeOriginAndDestination(newOrigin: LatLon, newDestination: LatLon)
      extends BookingCommand
  final case object Cancel extends BookingCommand
}
