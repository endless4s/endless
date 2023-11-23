package endless.example.logic

import cats.Monad
import endless.core.entity.Sharding
import endless.example.algebra.{VehicleAlg, VehiclesAlg}
import endless.example.data.Vehicle.VehicleID

final case class ShardedVehicles[F[_]: Monad](sharding: Sharding[F, VehicleID, VehicleAlg])
    extends VehiclesAlg[F] {
  def vehicleFor(vehicleID: VehicleID): VehicleAlg[F] = sharding.entityFor(vehicleID)
}
