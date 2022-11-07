package endless.example.logic

import cats.syntax.either._
import endless.\/
import endless.core.event.EventApplier
import endless.example.data.BookingEvent._
import endless.example.data.{Booking, BookingEvent}

//#definition
class BookingEventApplier extends EventApplier[Booking, BookingEvent] {
  def apply(state: Option[Booking], event: BookingEvent): String \/ Option[Booking] = ???
}
//#definition
