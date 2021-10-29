package endless.core.interpret

import cats.Applicative
import cats.data.ReaderWriterStateT
import endless.core.typeclass.entity.Effector

import scala.concurrent.duration.FiniteDuration

object EffectorT extends LoggerLiftingHelpers {
  def unit[F[_]: Applicative, S]: EffectorT[F, S, Unit] =
    ReaderWriterStateT.liftF(Applicative[F].unit)

  /** `EffectorT[F, S, A]` is a type alias for [[ReaderWriterStateT]] monad transformer from cats.
    * It uses `Reader` to allow access to ready-only entity state and `State` to update the
    * passivation activation schedule (`Writer` is unused)
    * @tparam F
    *   context
    * @tparam S
    *   entity state
    * @tparam A
    *   value
    */
  type EffectorT[F[_], S, A] = ReaderWriterStateT[F, Option[S], Unit, PassivationState, A]

  sealed trait PassivationState
  object PassivationState {
    final case class After(duration: FiniteDuration) extends PassivationState
    object Disabled extends PassivationState
  }

  trait EffectorLift[G[_], F[_], S] extends Effector[G, S]

  implicit def instance[F[_]: Applicative, S]: EffectorLift[EffectorT[F, S, *], F, S] =
    new EffectorLift[EffectorT[F, S, *], F, S] {
      def read: EffectorT[F, S, Option[S]] = ReaderWriterStateT.ask

      def enablePassivation(after: FiniteDuration): EffectorT[F, S, Unit] =
        ReaderWriterStateT.set(PassivationState.After(after))

      def disablePassivation: EffectorT[F, S, Unit] =
        ReaderWriterStateT.set(PassivationState.Disabled)
    }

}
