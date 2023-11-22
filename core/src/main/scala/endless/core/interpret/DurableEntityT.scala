package endless.core.interpret

import cats.data.{IndexedStateT, StateT}
import cats.effect.kernel.Clock
import cats.{Applicative, Functor, Monad, ~>}
import endless.core.entity.DurableEntity
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.FiniteDuration

object DurableEntityT {
  sealed trait State[+S]
  object State {
    case object None extends State[Nothing]
    final case class Existing[S](state: S) extends State[S]
    final case class Updated[S](state: S) extends State[S]
  }

  /** `DurableEntityT[F, S, A]` is a type alias for StateT monad transformer from cats. `State` is
    * the state of the entity, which can be get (exposed as `read`) and set (exposed as `write`)
    * @tparam F
    *   context
    * @tparam S
    *   entity state
    * @tparam A
    *   value
    */
  type DurableEntityT[F[_], S, A] = StateT[F, State[S], A]

  def liftF[F[_]: Applicative, S, A](fa: F[A]): DurableEntityT[F, S, A] = StateT.liftF(fa)

  implicit def liftK[F[_]: Applicative, S]: F ~> DurableEntityT[F, S, *] = StateT.liftK

  def unit[F[_]: Applicative, S]: DurableEntityT[F, S, Unit] = liftF(Applicative[F].unit)

  def stateReader[F[_]: Applicative, S]: DurableEntityT[F, S, Option[S]] =
    StateT.get[F, State[S]].map {
      case State.None            => None
      case State.Existing(state) => Some(state)
      case State.Updated(state)  => Some(state)
    }

  def stateWriter[F[_]: Applicative, S](state: S): DurableEntityT[F, S, Unit] =
    StateT.set(State.Updated(state))

  def stateModifier[F[_]: Applicative, S](f: S => S): DurableEntityT[F, S, Unit] =
    StateT.modify {
      case State.None            => State.None
      case State.Existing(state) => State.Updated(f(state))
      case State.Updated(state)  => State.Updated(f(state))
    }

  def stateModifierF[F[_]: Applicative, S](f: S => F[S]): DurableEntityT[F, S, Unit] =
    StateT.modifyF {
      case State.None            => Applicative[F].pure(State.None)
      case State.Existing(state) => Functor[F].map(f(state))(State.Updated(_))
      case State.Updated(state)  => Functor[F].map(f(state))(State.Updated(_))
    }

  /** Given that a monad instance can be found for F, this provides an DurableEntityT transformer
    * instance for it. This is used by `deployDurableEntity`: the `createEntity` creator for entity
    * algebra can thus be injected with an instance of `DurableEntity[F[_]]` interpreted with
    * DurableEntityT[F, S, *]
    */
  implicit def instance[F[_]: Monad, S]: DurableEntity[DurableEntityT[F, S, *], S] =
    new DurableEntity[DurableEntityT[F, S, *], S] {
      def read: DurableEntityT[F, S, Option[S]] = stateReader
      def write(s: S): DurableEntityT[F, S, Unit] = stateWriter(s)
      def modify(f: S => S): DurableEntityT[F, S, Unit] = stateModifier(f)
      def modifyF(f: S => DurableEntityT[F, S, S]): DurableEntityT[F, S, Unit] =
        stateModifierF(state => f(state).runA(State.Updated(state)))

      implicit lazy val monad: Monad[DurableEntityT[F, S, *]] =
        IndexedStateT.catsDataMonadForIndexedStateT
    }

  implicit def clockForDurableEntityT[F[_]: Applicative: Clock, S](implicit
      A0: Applicative[DurableEntityT[F, S, *]]
  ): Clock[DurableEntityT[F, S, *]] =
    new Clock[DurableEntityT[F, S, *]] {
      def applicative: Applicative[DurableEntityT[F, S, *]] = A0

      def monotonic: DurableEntityT[F, S, FiniteDuration] = liftF(Clock[F].monotonic)

      def realTime: DurableEntityT[F, S, FiniteDuration] = liftF(Clock[F].realTime)
    }

  implicit def loggerForDurableEntityT[F[_]: Applicative, S](implicit
      logger: Logger[F]
  ): Logger[DurableEntityT[F, S, *]] = logger.mapK(liftK[F, S])
}
