package endless.example.data

import cats.Show
import cats.implicits.toContravariantOps
import endless.example.data.Booking.*

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
  final case class BookingID(id: UUID)
  object BookingID {
    def fromString(str: String): BookingID = BookingID(UUID.fromString(str))
    implicit val show: Show[BookingID] = Show.catsShowForUUID.contramap(_.id)
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
