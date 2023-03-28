package endless.example.logic

import cats.effect.Ref
import cats.effect.kernel.Sync
import cats.syntax.functor._
import cats.{Applicative, Monad}
import endless.core.entity.Effector
import endless.core.interpret.EffectorT
import endless.core.interpret.EffectorT.EffectorT
import endless.example.algebra.VehicleAlg
import endless.example.data.Vehicle
import endless.example.logic.VehicleEffector.Interpreted

import scala.concurrent.duration._

class VehicleEffector[F[_]: Monad](
    effector: Effector[Interpreted[F, *], Vehicle, VehicleAlg],
    justRecoveredRef: Ref[F, Boolean]
) {
  private lazy val aggressivePassivation = effector.enablePassivation(1.second)

  def apply: Interpreted[F, Unit] = {
    for {
      justRecovered <- EffectorT.liftF(justRecoveredRef.getAndUpdate(_ => false))
      _ <- Applicative[Interpreted[F, *]].whenA(justRecovered)(
        effector.self.flatMap(_.incrementRecoveryCount)
      )
      _ <- aggressivePassivation
    } yield ()
  }
}

object VehicleEffector {
  type Interpreted[F[_], A] = EffectorT[F, Vehicle, VehicleAlg, A]

  def apply[F[_]: Sync](
      effector: Effector[Interpreted[F, *], Vehicle, VehicleAlg]
  ): F[VehicleEffector[F]] =
    Ref[F].of(true).map(justRecovered => new VehicleEffector[F](effector, justRecovered))
}
