package endless.core.typeclass.effect

import cats.Applicative
import endless.core.typeclass.entity.StateReader

/** `Effector` is the ability to carry out a side effect in the system after successful event
  * persistence (e.g. asynchronous message write or request to some external system)
  */
trait Effector[F[_]] {
  def afterPersist: F[Unit]
}

object Effector {

  /** Default effector, does nothing
    */
  def unit[F[_]: Applicative, S]: StateReader[F, S] => Effector[F] = UnitEffector(_)
  private final case class UnitEffector[F[_]: Applicative, S](reader: StateReader[F, S])
      extends Effector[F] {
    override def afterPersist: F[Unit] = Applicative[F].unit
  }
}
