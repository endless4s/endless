package endless.example.algebra

import endless.example.data.Vehicle.VehicleID

//#definition
trait VehicleRepositoryAlg[F[_]] {
  def vehicleFor(vehicleID: VehicleID): VehicleAlg[F]
}
//#definition
