package endless.example.data

import cats.{Eq, Show}
import endless.example.data.Booking._

import java.time.Instant
import java.util.UUID

final case class Booking(
    id: BookingID,
    time: Instant,
    origin: LatLon,
    destination: LatLon,
    passengerCount: Int,
    status: Booking.Status = Booking.Status.Pending
)

object Booking {
  final case class BookingID(id: UUID) extends AnyVal
  object BookingID {
    def fromString(str: String): BookingID = BookingID(UUID.fromString(str))
    implicit val show: Show[BookingID] = Show.show(_.id.toString)
  }
  final case class LatLon(lat: Double, lon: Double)
  object LatLon {
    implicit val eq: Eq[LatLon] = Eq.fromUniversalEquals
  }
  sealed trait Status
  object Status {
    case object Pending extends Status
    case object Accepted extends Status
    case object Rejected extends Status
    case object Cancelled extends Status
  }
  implicit val show: Show[Booking] = Show.fromToString
}
