package endless.example.logic

import cats.Monad
import endless.core.entity.Repository
import endless.example.algebra.{VehicleAlg, VehicleRepositoryAlg}
import endless.example.data.Vehicle.VehicleID

final case class VehicleRepository[F[_]: Monad](repository: Repository[F, VehicleID, VehicleAlg])
    extends VehicleRepositoryAlg[F] {
  def vehicleFor(vehicleID: VehicleID): VehicleAlg[F] = repository.entityFor(vehicleID)
}
