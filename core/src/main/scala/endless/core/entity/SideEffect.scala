package endless.core.entity
import cats.Applicative
import cats.kernel.Eq
import cats.syntax.eq.*

/** `SideEffect[F, S, Alg]` represents a side-effect applied in context `F`. The side-effect is
  * triggered just after events persistence if any, or after some reads for a read-only command. The
  * mode of interpretation is determined by the `runModeFor` method, which defaults to `Async` but
  * can be overridden. The passed `Effector` can be used to access entity state and algebra and to
  * control passivation.
  * @tparam F
  *   effect type
  * @tparam S
  *   entity state
  * @tparam Alg
  *   entity algebra
  */
trait SideEffect[F[_], S, Alg[_[_]]] {
  def apply(trigger: SideEffect.Trigger, effector: Effector[F, S, Alg]): F[Unit]

  def runModeFor(trigger: SideEffect.Trigger, state: Option[S])(implicit
      applicative: Applicative[F]
  ): F[SideEffect.RunMode] = Applicative[F].pure(SideEffect.RunMode.Async)
}

object SideEffect {
  def unit[F[_]: Applicative, S, Alg[_[_]]]: SideEffect[F, S, Alg] =
    (_: Trigger, _: Effector[F, S, Alg]) => Applicative[F].unit

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

    implicit val eqTrigger: Eq[Trigger] = Eq.fromUniversalEquals
  }

  /** Run mode for a side-effect: `Async` (default value) means that the side-effect is triggered in
    * "fire & forget" mode, while `Sync` means it is run to completion before any other command is
    * processed by the entity.
    */
  sealed trait RunMode
  object RunMode {

    /** Run to completion before any other command is processed by the entity.
      *
      * @note
      *   This mode should in most cases not be used for long-running side-effects, as it can hurt
      *   availability of the entity for command processing.
      */
    case object Sync extends RunMode

    /** Run in "fire & forget" mode.
      *
      * @note
      *   This mode requires careful consideration of the side-effect's concurrency and idempotency,
      *   as there is no limit on the number of invocations running simultaneously at any one time.
      */
    case object Async extends RunMode
  }
}
