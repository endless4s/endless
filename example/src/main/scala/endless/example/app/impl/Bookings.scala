package endless.example.app.impl

import cats.syntax.show.*
import endless.core.entity.EntityNameProvider
import endless.core.protocol.EntityIDCodec
import endless.example.adapter.BookingEventAdapter
import endless.example.data.Booking.BookingID
import endless.example.logic.BookingEventApplier
import endless.example.protocol.BookingCommandProtocol

trait Bookings {
  implicit val bookingCommandProtocol: BookingCommandProtocol = new BookingCommandProtocol
  implicit val bookingEntityNameProvider: EntityNameProvider[BookingID] = () => "booking"
  implicit val bookingIDEncoder: EntityIDCodec[BookingID] =
    EntityIDCodec(_.id.show, BookingID.fromString)
  implicit val eventApplier: BookingEventApplier = new BookingEventApplier
  val eventAdapter: BookingEventAdapter = new BookingEventAdapter
}
