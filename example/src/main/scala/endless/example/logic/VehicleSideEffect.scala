package endless.example.logic

import cats.{Applicative, Monad}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import endless.core.entity.SideEffect.Trigger
import endless.core.entity.{Effector, SideEffect}
import endless.example.algebra.VehicleAlg
import endless.example.data.Vehicle

import scala.concurrent.duration.*

class VehicleSideEffect[F[_]: Monad] extends SideEffect[F, Vehicle, VehicleAlg] {
  def apply(trigger: Trigger, effector: Effector[F, Vehicle, VehicleAlg]): F[Unit] = {
    lazy val aggressivePassivation = effector.enablePassivation(1.second)
    for {
      _ <- Applicative[F].whenA(trigger.isAfterRecovery)(effector.self.incrementRecoveryCount)
      _ <- aggressivePassivation
    } yield ()
  }
}
