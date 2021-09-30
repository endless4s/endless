package endless.core.interpret

import cats.conversions.all._
import cats.data.{Chain, NonEmptyChain}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.{Applicative, Monad}
import endless.core.data.EventsFolder
import endless.core.data.Folded

/** This monad transformer implements the capability to run a sequence of computations within a
  * context equipped with an event application function and a chain of events. It provides the
  * implementations of `read` of `StateReader` and `write` of `EventWriter` allowing to run monadic
  * chains making use of the `Entity` typeclass.
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

  def reader[F[_]: Monad, S, E]: EntityT[F, S, E, S] = new EntityT(read[F, S, E])

  implicit def instance[F[_], S, E](implicit
      monad0: Monad[F]
  ): EntityLift[EntityT[F, S, E, *], F, S, E] =
    new EntityTLiftInstance[F, S, E] {
      override protected implicit def monad: Monad[F] = monad0
    }
}
