package endless.core.interpret

import cats.conversions.all.*
import cats.data.{Chain, NonEmptyChain}
import cats.effect.kernel.Clock
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.{Applicative, Functor, Monad, ~>}
import endless.core.data.{EventsFolder, Folded}
import endless.core.entity.Entity
import endless.core.event.EventApplier
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.FiniteDuration

/** `EntityT[F, S, E, A]`` is data type implementing the `Entity[F, S, E]` state reader and event
  * writer abilities. It is a monad transformer used as an interpreter for functional chains
  * involving calls to `Entity` read` and `write`, turning them into a result value of `F[Folded[E,
  * A]]`. `Folded[E, A]` is either an error or a list of events bundled together with a result
  * value.
  *
  * `EntityT` interpretation runs with an instance of `EventsFolder[S, E]` which is a tuple of
  * current state of type `S` together with event application function `EventApplier[S, E]`.
  * Interpretation essentially accumulates the written events into a `Chain[E]` and applies these
  * events to initial state whenever a `read` is required.
  * @param runAcc
  *   Event folding function
  * @tparam F
  *   context
  * @tparam S
  *   state
  * @tparam E
  *   event
  * @tparam A
  *   value
  */
final class EntityT[F[_], S, E, A](
    val runAcc: (EventsFolder[S, E], Chain[E]) => F[Folded[E, A]]
) extends AnyVal {
  def run(state: Option[S])(implicit applier: EventApplier[S, E]): F[Folded[E, A]] = runWithFolder(
    EventsFolder(state, applier)
  )

  private def runWithFolder(folder: EventsFolder[S, E]): F[Folded[E, A]] =
    runAcc(folder, Chain.empty)

  def flatMap[B](f: A => EntityT[F, S, E, B])(implicit monad: Monad[F]): EntityT[F, S, E, B] =
    new EntityT[F, S, E, B]((folder, events) => {
      runAcc(folder, events).flatMap {
        case Right((accEvents, a)) => f(a).runAcc(folder, accEvents)
        case Left(reason)          => reason.asLeft.pure
      }
    })

  def map[B](f: A => B)(implicit monad: Monad[F]): EntityT[F, S, E, B] =
    flatMap(a => EntityT.purr(f(a)))
}

object EntityT extends EntityRunFunctions {
  def writer[F[_]: Applicative, S, E](newEvents: NonEmptyChain[E]): EntityT[F, S, E, Unit] =
    new EntityT((_, existing) => write(newEvents)(existing))

  def purr[F[_]: Applicative, S, E, A](a: A): EntityT[F, S, E, A] = new EntityT((_, events) =>
    pure(a)(events)
  )

  def liftF[F[_]: Functor, S, E, A](fa: F[A]): EntityT[F, S, E, A] =
    new EntityT((_, events) => fa.map(a => (events, a).asRight))

  implicit def liftK[F[_]: Functor, S, E]: F ~> EntityT[F, S, E, *] =
    new (F ~> EntityT[F, S, E, *]) {
      def apply[B](fa: F[B]): EntityT[F, S, E, B] = liftF(fa)
    }

  def reader[F[_]: Monad, S, E]: EntityT[F, S, E, Option[S]] = new EntityT(read[F, S, E])

  /** Given that a monad instance can be found for F, this provides an EntityT transformer instance
    * for it. This is used by `deployEntity`: the `createEntity` creator for entity algebra can thus
    * be injected with an instance of `Entity[F[_]]` interpreted with EntityT[F, S, E, *]
    */
  implicit def instance[F[_]: Monad, S, E]
      : Entity[EntityT[F, S, E, *], S, E] & Monad[EntityT[F, S, E, *]] =
    new EntityTLiftInstance[F, S, E]

  implicit def clockForEntityT[F[_]: Functor: Clock, S, E](implicit
      A0: Applicative[EntityT[F, S, E, *]]
  ): Clock[EntityT[F, S, E, *]] =
    new Clock[EntityT[F, S, E, *]] {
      def applicative: Applicative[EntityT[F, S, E, *]] = A0
      def monotonic: EntityT[F, S, E, FiniteDuration] = liftF(Clock[F].monotonic)
      def realTime: EntityT[F, S, E, FiniteDuration] = liftF(Clock[F].realTime)
    }

  implicit def loggerForEntityT[F[_]: Functor, S, E](implicit
      logger: Logger[F]
  ): Logger[EntityT[F, S, E, *]] = logger.mapK(liftK[F, S, E])
}
