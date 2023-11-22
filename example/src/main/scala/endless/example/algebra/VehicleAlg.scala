package endless.example.algebra

import endless.example.data.{LatLon, Speed}

//#definition
trait VehicleAlg[F[_]] {
  def setSpeed(speed: Speed): F[Unit]
  def setPosition(position: LatLon): F[Unit]
  def getSpeed: F[Option[Speed]]
  def getPosition: F[Option[LatLon]]
  def getRecoveryCount: F[Int]
  def incrementRecoveryCount: F[Unit]
}
//#definition
