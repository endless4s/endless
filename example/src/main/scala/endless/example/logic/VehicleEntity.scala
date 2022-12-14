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

  def setSpeed(speed: Speed): F[Unit] =
    ifKnownElse(_ =>
      Logger[F].info(show"Update speed to $speed") >> modify(_.copy(speed = Some(speed)))
    )(
      Logger[F].info(show"New vehicle with speed $speed") >> write(
        Vehicle(position = None, Some(speed))
      )
    )

  def setPosition(position: LatLon): F[Unit] =
    ifKnownElse(_ =>
      Logger[F].info(show"Update position to $position") >> modify(
        _.copy(position = Some(position))
      )
    )(
      Logger[F].info(show"New vehicle with position $position") >> write(
        Vehicle(position = Some(position), None)
      )
    )

  def getSpeed: F[Option[Speed]] = read.map(_.flatMap(_.speed))

  def getPosition: F[Option[LatLon]] = read.map(_.flatMap(_.position))

  def getRecoveryCount: F[Int] = read.map(_.map(_.recoveryCount).getOrElse(0))

  def incrementRecoveryCount: F[Unit] =
    modify(vehicle => vehicle.copy(recoveryCount = vehicle.recoveryCount + 1))
}
