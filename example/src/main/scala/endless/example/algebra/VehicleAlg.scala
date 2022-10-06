package endless.example.algebra

import cats.tagless.{Derive, FunctorK}
import endless.example.data.{LatLon, Speed}

//#definition
trait VehicleAlg[F[_]] {
  def setSpeed(speed: Speed): F[Unit]
  def setPosition(position: LatLon): F[Unit]
  def getSpeed: F[Option[Speed]]
  def getPosition: F[Option[LatLon]]
}
//#definition

object VehicleAlg {
  implicit lazy val functorKInstance: FunctorK[VehicleAlg] = Derive.functorK[VehicleAlg]
}
