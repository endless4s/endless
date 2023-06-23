package endless.example.adapter

import endless.example.data.{LatLon, Speed, Vehicle}
import endless.example.proto.vehicle.models.{LatLonV1, SpeedV1}
import endless.example.proto.vehicle.state.VehicleStateV1

class VehicleStateAdapter {
  def toJournal(state: Option[Vehicle]): Any = VehicleStateV1(
    state.flatMap(_.position.map(latLon => LatLonV1(latLon.lat, latLon.lon))),
    state.flatMap(_.speed.map(s => SpeedV1(s.metersPerSecond))),
    state.map(_.recoveryCount).getOrElse(0)
  )

  def fromJournal(from: Any): Option[Vehicle] = from match {
    case VehicleStateV1(position, speed, recoveryCount, _) =>
      Some(
        Vehicle(
          position.map(latLon => LatLon(latLon.lat, latLon.lon)),
          speed.map(_.metersPerSecond).map(Speed(_)),
          recoveryCount
        )
      )
  }
}
