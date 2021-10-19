package endless.core.interpret

import cats.conversions.all._
import cats.data.{Chain, NonEmptyChain}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, Functor, Monad}
import endless.core.data.EventsFolder
import endless.core.data.Folded

/** 'EntityT[F, S, E, A]' is data type implementing the `Entity[F, S, E]` state reader and event
  * writer abilities. It is a monad transformer used as an interpreter for functional chains
  * involving calls to 'Entity' `read` and `write`, turning them into a result value of `F[Folded[E,
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
  def run(folder: EventsFolder[S, E]): F[Folded[E, A]] =
    runAcc(folder, Chain.empty)

  def flatMap[B](f: A => EntityT[F, S, E, B])(implicit monad: Monad[F]): EntityT[F, S, E, B] =
    new EntityT[F, S, E, B]((folder, events) => {
      runAcc(folder, events).flatMap {
        case Right((accEvents, a)) => f(a).runAcc(folder, accEvents)
        case Left(reason)          => reason.asLeft.pure
      }
    })
}

object EntityT extends EntityRunFunctions {
  def writer[F[_]: Applicative, S, E](newEvents: NonEmptyChain[E]): EntityT[F, S, E, Unit] =
    new EntityT((_, existing) => write(newEvents)(existing))

  def purr[F[_]: Applicative, S, E, A](a: A): EntityT[F, S, E, A] = new EntityT((_, events) =>
    pure(a)(events)
  )

  def liftF[F[_]: Functor, S, E, A](fa: F[A]): EntityT[F, S, E, A] =
    new EntityT((_, events) => fa.map(a => (events, a).asRight))

  def reader[F[_]: Monad, S, E]: EntityT[F, S, E, Option[S]] = new EntityT(read[F, S, E])

  implicit def instance[F[_], S, E](implicit
      monad0: Monad[F]
  ): EntityLift[EntityT[F, S, E, *], F, S, E] =
    new EntityTLiftInstance[F, S, E] {
      override protected implicit def monad: Monad[F] = monad0
    }
}
