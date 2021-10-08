package endless.core.typeclass.effect

import cats.{Applicative, Id}
import endless.core.typeclass.entity.StateReader

trait Effector[F[_]] {
  def afterPersist: F[Unit]
}

object Effector {
  def unit[F[_]: Applicative, S]: StateReader[F, S] => Effector[F] = UnitEffector(_)
  private final case class UnitEffector[F[_]: Applicative, S](reader: StateReader[F, S])
      extends Effector[F] {
    override def afterPersist: F[Unit] = Applicative[F].unit
  }
}
