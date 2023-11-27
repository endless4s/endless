package endless.example.adapter

import com.google.protobuf.timestamp.Timestamp
import endless.example.data.{Booking, BookingEvent, LatLon}
import endless.example.proto.booking.events.*
import endless.example.proto.booking.models.*
import cats.syntax.show.*
import endless.example.proto.booking.{events => proto}

class BookingEventAdapter {
  def toJournal(event: BookingEvent): proto.BookingEvent =
    event match {
      case BookingEvent.BookingPlaced(bookingID, time, origin, destination, passengerCount) =>
        proto.BookingPlacedV1(
          BookingID(bookingID.show),
          Timestamp(time.getEpochSecond, time.getNano),
          LatLonV1(origin.lat, origin.lon),
          LatLonV1(destination.lat, destination.lon),
          passengerCount
        )
      case BookingEvent.OriginChanged(newOrigin) =>
        proto.OriginChangedV1(LatLonV1(newOrigin.lat, newOrigin.lon))
      case BookingEvent.DestinationChanged(newDestination) =>
        proto.DestinationChangedV1(LatLonV1(newDestination.lat, newDestination.lon))
      case BookingEvent.BookingAccepted  => proto.BookingAcceptedV1()
      case BookingEvent.BookingRejected  => proto.BookingRejectedV1()
      case BookingEvent.BookingCancelled => proto.BookingCanceledV1()
    }

  def fromJournal(event: proto.BookingEvent): BookingEvent = event match {
    case BookingPlacedV1(bookingID, time, origin, destination, passengerCount, _) =>
      BookingEvent.BookingPlaced(
        Booking.BookingID(java.util.UUID.fromString(bookingID.value)),
        java.time.Instant.ofEpochSecond(time.seconds, time.nanos),
        LatLon(origin.lat, origin.lon),
        LatLon(destination.lat, destination.lon),
        passengerCount
      )
    case OriginChangedV1(newOrigin, _) =>
      BookingEvent.OriginChanged(LatLon(newOrigin.lat, newOrigin.lon))
    case DestinationChangedV1(newDestination, _) =>
      BookingEvent.DestinationChanged(LatLon(newDestination.lat, newDestination.lon))
    case BookingAcceptedV1(_) => BookingEvent.BookingAccepted
    case BookingRejectedV1(_) => BookingEvent.BookingRejected
    case BookingCanceledV1(_) => BookingEvent.BookingCancelled
  }
}
