package endless.example.data

import cats.Show

import java.util.UUID

final case class Vehicle(position: Option[LatLon], speed: Option[Speed], recoveryCount: Int = 0)

object Vehicle {
  final case class VehicleID(id: UUID) extends AnyVal
  object VehicleID {
    def fromString(str: String): VehicleID = VehicleID(UUID.fromString(str))
    implicit val show: Show[VehicleID] = Show.show(_.id.toString)
  }
}
