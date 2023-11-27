package endless.core.entity
import cats.effect.kernel.{Concurrent, Ref}
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.{Applicative, Monad}

import scala.concurrent.duration.FiniteDuration

/** `Effector` represents the ability to read the state of the entity, perform a possible
  * passivation side-effect and further interact with the entity itself via its algebra.
  * @tparam F
  *   context
  * @tparam S
  *   state
  */
trait Effector[F[_], S, Alg[_[_]]] extends StateReader[F, S] with Passivator[F] with Self[F, Alg] {
  def ifKnown(f: S => F[Unit])(implicit monad: Monad[F]): F[Unit] =
    read >>= (_.fold(Applicative[F].unit)(f))
}

object Effector {
  def apply[F[_]: Concurrent, S, Alg[_[_]]](
      entityAlg: Alg[F],
      state: Option[S]
  ): F[Effector[F, S, Alg]] =
    for {
      passivationStateRef <- Ref.of[F, PassivationState](PassivationState.Unchanged)
    } yield new Effector[F, S, Alg] {
      def read: F[Option[S]] = state.pure[F]
      def enablePassivation(after: FiniteDuration): F[Unit] =
        passivationStateRef.set(PassivationState.After(after))
      def disablePassivation: F[Unit] = passivationStateRef.set(PassivationState.Disabled)
      def passivationState: F[PassivationState] = passivationStateRef.get
      def self: Alg[F] = entityAlg
    }

  sealed trait PassivationState
  object PassivationState {
    final case class After(duration: FiniteDuration) extends PassivationState
    object Disabled extends PassivationState
    object Unchanged extends PassivationState
  }
}
