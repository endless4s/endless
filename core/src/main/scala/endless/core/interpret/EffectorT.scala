package endless.core.interpret

import cats.data.ReaderWriterStateT
import cats.tagless.FunctorK
import cats.tagless.syntax.functorK._
import cats.{Applicative, Monad, ~>}
import endless.core.typeclass.entity.Effector

import scala.concurrent.duration.FiniteDuration

object EffectorT extends LoggerLiftingHelper {
  def unit[F[_]: Applicative, S, Alg[_[_]]]: EffectorT[F, S, Alg, Unit] =
    liftF(Applicative[F].unit)

  def stateReader[F[_]: Applicative, S, Alg[_[_]]]: EffectorT[F, S, Alg, Option[S]] =
    ReaderWriterStateT.ask[F, Env[S, Alg[F]], Unit, PassivationState].map(_.state)

  def entityAlgReader[F[_]: Applicative, S, Alg[_[_]]]: EffectorT[F, S, Alg, Alg[F]] =
    ReaderWriterStateT.ask[F, Env[S, Alg[F]], Unit, PassivationState].map(_.entity)

  def passivationEnabler[F[_]: Applicative, S, Alg[_[_]]](
      after: FiniteDuration
  ): EffectorT[F, S, Alg, Unit] =
    ReaderWriterStateT.set(PassivationState.After(after))

  def passivationDisabler[F[_]: Applicative, S, Alg[_[_]]]: EffectorT[F, S, Alg, Unit] =
    ReaderWriterStateT.set(PassivationState.Disabled)

  /** `EffectorT[F, S, A]` is a type alias for `ReaderWriterStateT` monad transformer from cats. It
    * uses `Reader` to allow access to ready-only entity state and algebra and `State` to update the
    * passivation activation schedule (`Writer` is unused)
    * @tparam F
    *   context
    * @tparam S
    *   entity state
    * @tparam A
    *   value
    */
  type EffectorT[F[_], S, Alg[_[_]], A] =
    ReaderWriterStateT[F, Env[S, Alg[F]], Unit, PassivationState, A]

  implicit class EffectorTRunHelpers[F[_]: Monad, S, Alg[_[_]], A](
      val effectorT: EffectorT[F, S, Alg, A]
  ) {
    def runA(entityState: Option[S], entity: Alg[F]): F[A] =
      effectorT.runA(Env(entityState, entity), PassivationState.Disabled)
    def runS(entityState: Option[S], entity: Alg[F]): F[PassivationState] =
      effectorT.runS(Env(entityState, entity), PassivationState.Disabled)
  }

  final case class Env[S, Alg](state: Option[S], entity: Alg)

  sealed trait PassivationState
  object PassivationState {
    final case class After(duration: FiniteDuration) extends PassivationState
    object Disabled extends PassivationState
  }

  trait EffectorLift[G[_], F[_], S, Alg[_[_]]] extends Effector[G, S, Alg]

  implicit def instance[F[_]: Applicative, S, Alg[_[_]]: FunctorK]
      : EffectorLift[EffectorT[F, S, Alg, *], F, S, Alg] =
    new EffectorLift[EffectorT[F, S, Alg, *], F, S, Alg] {
      def read: EffectorT[F, S, Alg, Option[S]] = stateReader[F, S, Alg]
      def enablePassivation(after: FiniteDuration): EffectorT[F, S, Alg, Unit] = passivationEnabler(
        after
      )
      def disablePassivation: EffectorT[F, S, Alg, Unit] = passivationDisabler
      def self: EffectorT[F, S, Alg, Alg[EffectorT[F, S, Alg, *]]] =
        entityAlgReader[F, S, Alg].map(_.mapK(liftK))
    }

  def liftF[F[_]: Applicative, S, Alg[_[_]], A](fa: F[A]): EffectorT[F, S, Alg, A] =
    ReaderWriterStateT.liftF(fa)

  implicit def liftK[F[_]: Applicative, S, Alg[_[_]], A]: F ~> EffectorT[F, S, Alg, *] =
    ReaderWriterStateT.liftK
}
