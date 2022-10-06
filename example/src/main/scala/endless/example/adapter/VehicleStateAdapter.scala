package endless.example.adapter

import akka.persistence.typed.SnapshotAdapter
import endless.example.data.{LatLon, Speed, Vehicle}
import endless.example.proto.vehicle.models.{LatLonV1Full, SpeedV1Full}
import endless.example.proto.vehicle.state.VehicleStateV1Full

class VehicleStateAdapter extends SnapshotAdapter[Option[Vehicle]] {
  def toJournal(state: Option[Vehicle]): Any = VehicleStateV1Full(
    state.flatMap(_.position.map(latLon => LatLonV1Full(latLon.lat, latLon.lon))),
    state.flatMap(_.speed.map(s => SpeedV1Full(s.metersPerSecond)))
  )

  def fromJournal(from: Any): Option[Vehicle] = from match {
    case VehicleStateV1Full(position, speed, _) =>
      Some(
        Vehicle(
          position.map(latLon => LatLon(latLon.lat, latLon.lon)),
          speed.map(_.metersPerSecond).map(Speed(_))
        )
      )
  }
}
