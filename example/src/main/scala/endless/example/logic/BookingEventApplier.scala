package endless.example.logic

import cats.syntax.either._
import endless.\/
import endless.core.typeclass.event.EventApplier
import endless.example.data.{Booking, BookingEvent}

class BookingEventApplier extends EventApplier[Option[Booking], BookingEvent] {
  def apply(state: Option[Booking], event: BookingEvent): String \/ Option[Booking] =
    event match {
      case BookingEvent.BookingPlaced(bookingID, origin, destination, passengerCount) =>
        Option(
          Booking(bookingID, origin, destination, passengerCount)
        ).asRight
      case BookingEvent.OriginChanged(newOrigin) =>
        state
          .toRight("Attempt to change unknown booking")
          .map(_.copy(origin = newOrigin))
          .map(Some(_))
      case BookingEvent.DestinationChanged(newDestination) =>
        state
          .toRight("Attempt to change unknown booking")
          .map(_.copy(destination = newDestination))
          .map(Some(_))
    }
}
