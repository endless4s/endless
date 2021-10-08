package endless.example.data

import endless.example.data.Booking._
import cats.{Eq, Show}

import java.util.UUID

final case class Booking(
    id: BookingID,
    origin: LatLon,
    destination: LatLon,
    passengerCount: Int
)

object Booking {
  final case class BookingID(id: UUID) extends AnyVal
  object BookingID {
    implicit val show: Show[BookingID] = Show.fromToString
  }
  final case class LatLon(lat: Double, lon: Double)
  object LatLon {
    implicit val eq: Eq[LatLon] = Eq.fromUniversalEquals
  }
  implicit val show: Show[Booking] = Show.fromToString
}
