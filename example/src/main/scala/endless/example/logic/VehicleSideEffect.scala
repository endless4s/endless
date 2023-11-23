package endless.example.logic

import cats.Applicative
import cats.effect.{Concurrent, Ref}
import cats.syntax.flatMap._
import cats.syntax.functor._
import endless.core.entity.{Effector, SideEffect}
import endless.example.algebra.VehicleAlg
import endless.example.data.Vehicle

import scala.concurrent.duration._

class VehicleSideEffect[F[_]: Concurrent](justRecoveredRef: Ref[F, Boolean])
    extends SideEffect[F, Vehicle, VehicleAlg] {
  def apply(effector: Effector[F, Vehicle, VehicleAlg]): F[Unit] = {
    lazy val aggressivePassivation = effector.enablePassivation(1.second)
    for {
      justRecovered <- justRecoveredRef.getAndUpdate(_ => false)
      _ <- Applicative[F].whenA(justRecovered)(effector.self.incrementRecoveryCount)
      _ <- aggressivePassivation
    } yield ()
  }
}

object VehicleSideEffect {
  def apply[F[_]: Concurrent](): F[SideEffect[F, Vehicle, VehicleAlg]] =
    Ref[F].of(true).map(new VehicleSideEffect[F](_))
}
