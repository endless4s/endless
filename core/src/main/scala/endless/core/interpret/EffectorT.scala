package endless.core.interpret

import cats.{Applicative, Functor, Monad, ~>}
import cats.data.ReaderWriterStateT
import cats.tagless.FunctorK
import cats.tagless.syntax.functorK._
import endless.core.interpret.EntityT.liftF
import endless.core.typeclass.entity.Effector

import scala.concurrent.duration.FiniteDuration

object EffectorT extends LoggerLiftingHelper {
  def unit[F[_]: Applicative, S]: EffectorT[F, S, Unit] =
    liftF(Applicative[F].unit)

  def reader[F[_]: Applicative, S]: EffectorT[F, S, Option[S]] = ReaderWriterStateT.ask

  def passivationEnabler[F[_]: Applicative, S](after: FiniteDuration): EffectorT[F, S, Unit] =
    ReaderWriterStateT.set(PassivationState.After(after))

  def passivationDisabler[F[_]: Applicative, S]: EffectorT[F, S, Unit] =
    ReaderWriterStateT.set(PassivationState.Disabled)

  /** `EffectorT[F, S, A]` is a type alias for `ReaderWriterStateT` monad transformer from cats. It
    * uses `Reader` to allow access to ready-only entity state and `State` to update the passivation
    * activation schedule (`Writer` is unused)
    * @tparam F
    *   context
    * @tparam S
    *   entity state
    * @tparam A
    *   value
    */
  type EffectorT[F[_], S, A] = ReaderWriterStateT[F, Option[S], Unit, PassivationState, A]

  implicit class EffectorTRunHelpers[F[_]: Monad, S, A](val effectorT: EffectorT[F, S, A]) {
    def runA(entityState: Option[S]): F[A] = effectorT.runA(entityState, PassivationState.Disabled)
    def runS(entityState: Option[S]): F[PassivationState] =
      effectorT.runS(entityState, PassivationState.Disabled)
  }

  sealed trait PassivationState
  object PassivationState {
    final case class After(duration: FiniteDuration) extends PassivationState
    object Disabled extends PassivationState
  }

  trait EffectorLift[G[_], F[_], S] extends Effector[G, S]

  implicit def instance[F[_]: Applicative, S]: EffectorLift[EffectorT[F, S, *], F, S] =
    new EffectorLift[EffectorT[F, S, *], F, S] {
      def read: EffectorT[F, S, Option[S]] = reader
      def enablePassivation(after: FiniteDuration): EffectorT[F, S, Unit] = passivationEnabler(
        after
      )
      def disablePassivation: EffectorT[F, S, Unit] = passivationDisabler
    }

  def liftF[F[_]: Applicative, S, A](fa: F[A]): EffectorT[F, S, A] =
    ReaderWriterStateT.liftF(fa)

  implicit def liftK[F[_]: Applicative, S, A]: F ~> EffectorT[F, S, *] = ReaderWriterStateT.liftK
}
