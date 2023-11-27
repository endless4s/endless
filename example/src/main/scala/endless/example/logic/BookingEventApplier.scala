package endless.example.logic

import cats.syntax.either.*
import endless.\/
import endless.core.event.EventApplier
import endless.example.data.BookingEvent.*
import endless.example.data.{Booking, BookingEvent}

//#definition
class BookingEventApplier extends EventApplier[Booking, BookingEvent] {
  def apply(state: Option[Booking], event: BookingEvent): String \/ Option[Booking] =
    (event match {
      case BookingPlaced(bookingID, time, origin, destination, passengerCount) =>
        state
          .toLeft(Booking(bookingID, time, origin, destination, passengerCount))
          .leftMap(_ => "Booking already exists")
      case OriginChanged(newOrigin) =>
        state
          .toRight("Attempt to change unknown booking")
          .map(_.copy(origin = newOrigin))
      case DestinationChanged(newDestination) =>
        state
          .toRight("Attempt to change unknown booking")
          .map(_.copy(destination = newDestination))
      case BookingAccepted =>
        state
          .toRight("Attempt to accept unknown booking")
          .map(_.copy(status = Booking.Status.Accepted))
      case BookingRejected =>
        state
          .toRight("Attempt to reject unknown booking")
          .map(_.copy(status = Booking.Status.Rejected))
      case BookingCancelled =>
        state
          .toRight("Attempt to cancel unknown booking")
          .map(_.copy(status = Booking.Status.Cancelled))
    }).map(Option(_))
}
//#definition
