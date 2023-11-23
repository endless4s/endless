package endless.example.algebra

import endless.example.data.Vehicle.VehicleID

//#definition
trait VehiclesAlg[F[_]] {
  def vehicleFor(vehicleID: VehicleID): VehicleAlg[F]
}
//#definition
