package endless.core.entity
import cats.syntax.eq.*

/** `SideEffect[F, S, Alg]` represents a side-effect applied in context `F`. The side-effect is
  * triggered just after events persistence if any, or after some reads for a read-only command. Its
  * is interpreted with `Async` in order to allow for asynchronous processes. The passed `Effector`
  * can be used to access entity state and algebra and to control passivation.
  * @tparam F
  *   effect type
  * @tparam S
  *   entity state
  * @tparam Alg
  *   entity algebra
  */
trait SideEffect[F[_], S, Alg[_[_]]] {
  def apply(trigger: SideEffect.Trigger, effector: Effector[F, S, Alg]): F[Unit]
}

object SideEffect {

  /** Trigger for the invocation of a side-effect: this allows for differentiated behavior depending
    * on the context in which the side-effect is triggered.
    */
  sealed trait Trigger {
    def isAfterPersistence: Boolean = this === Trigger.AfterPersistence
    def isAfterRead: Boolean = this === Trigger.AfterRead
    def isAfterRecovery: Boolean = this === Trigger.AfterRecovery
  }
  object Trigger {

    /** Triggered just after events or state persistence */
    case object AfterPersistence extends Trigger

    /** Triggered just after processing a read-only command (no events were written, the state
      * hasn't changed)
      */
    case object AfterRead extends Trigger

    /** Triggered just after recovery */
    case object AfterRecovery extends Trigger

    implicit val eqTrigger: cats.Eq[Trigger] = cats.Eq.fromUniversalEquals
  }
}
