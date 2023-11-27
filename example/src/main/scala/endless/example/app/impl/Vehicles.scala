package endless.example.app.impl

import cats.syntax.show.*
import endless.core.entity.EntityNameProvider
import endless.core.protocol.EntityIDCodec
import endless.example.adapter.VehicleStateAdapter
import endless.example.data.Vehicle.VehicleID
import endless.example.protocol.VehicleCommandProtocol

trait Vehicles {
  implicit val vehicleCommandProtocol: VehicleCommandProtocol = new VehicleCommandProtocol
  implicit val vehicleEntityNameProvider: EntityNameProvider[VehicleID] = () => "vehicle"
  implicit val vehicleIDEncoder: EntityIDCodec[VehicleID] =
    EntityIDCodec(_.id.show, VehicleID.fromString)
  val stateAdapter = new VehicleStateAdapter
}
