package endless.example.logic

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import endless.core.entity.DurableEntity
import endless.example.algebra.VehicleAlg
import endless.example.data.{LatLon, Speed, Vehicle}
import org.typelevel.log4cats.Logger

final case class VehicleEntity[F[_]: Logger](entity: DurableEntity[F, Vehicle])
    extends VehicleAlg[F] {
  import entity._

  def setSpeed(speed: Speed): F[Unit] = ???

  def setPosition(position: LatLon): F[Unit] = ???

  def getSpeed: F[Option[Speed]] = ???

  def getPosition: F[Option[LatLon]] = ???
}
