package endless.example.logic

import cats.Applicative
import cats.effect.{Concurrent, Ref}
import cats.syntax.flatMap._
import cats.syntax.functor._
import endless.core.entity.Effector
import endless.core.interpret.EffectorInterpreter
import endless.example.algebra.{VehicleAlg, VehicleRepositoryAlg}
import endless.example.data.Vehicle

import scala.concurrent.duration._

object VehicleEffector {
  def apply[F[_]: Concurrent]
      : F[EffectorInterpreter[F, Vehicle, VehicleAlg, VehicleRepositoryAlg]] =
    Ref[F]
      .of(true)
      .map(justRecoveredRef =>
        (
            effector: Effector[F, Vehicle, VehicleAlg],
            _: VehicleRepositoryAlg[F],
            _: VehicleAlg[F]
        ) => {
          lazy val aggressivePassivation = effector.enablePassivation(1.second)
          for {
            justRecovered <- justRecoveredRef.getAndUpdate(_ => false)
            _ <- Applicative[F].whenA(justRecovered)(effector.self.incrementRecoveryCount)
            _ <- aggressivePassivation
          } yield ()
        }
      )
}
