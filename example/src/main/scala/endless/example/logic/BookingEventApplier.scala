package endless.example.logic

import cats.syntax.either._
import endless.\/
import endless.core.typeclass.event.EventApplier
import endless.example.data.{Booking, BookingEvent}
import BookingEvent._

class BookingEventApplier extends EventApplier[Booking, BookingEvent] {
  def apply(state: Option[Booking], event: BookingEvent): String \/ Option[Booking] =
    (event match {
      case BookingPlaced(bookingID, origin, destination, passengerCount) =>
        state
          .toLeft(Booking(bookingID, origin, destination, passengerCount))
          .leftMap(_ => "Booking already exists")
      case OriginChanged(newOrigin) =>
        state
          .toRight("Attempt to change unknown booking")
          .map(_.copy(origin = newOrigin))
      case DestinationChanged(newDestination) =>
        state
          .toRight("Attempt to change unknown booking")
          .map(_.copy(destination = newDestination))
      case BookingCancelled =>
        state.toRight("Attempt to cancel unknown booking").map(_.copy(cancelled = true))
    }).map(Option(_))
}
