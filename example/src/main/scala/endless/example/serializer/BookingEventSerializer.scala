package endless.example.serializer

import endless.circe.serializer.CirceSerializer
import endless.example.data.BookingEvent
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto._

class BookingEventSerializer extends CirceSerializer[BookingEvent] {
  def identifier: Int = 42
  def encoder: Encoder[BookingEvent] = deriveEncoder[BookingEvent]
  def decoder: Decoder[BookingEvent] = deriveDecoder[BookingEvent]
}
