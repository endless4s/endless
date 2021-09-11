package akka.persistence.tagless.core.interpret

import akka.persistence.tagless.core.data.{EventsFolder, Folded}
import cats.data.{Chain, NonEmptyChain}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.conversions.all._
import cats.{Applicative, Functor, Monad}

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

  def liftF(fa: F[A])(implicit functor: Functor[F]): EntityT[F, S, E, A] = EntityT.lifter(fa)

  def read(implicit monad: Monad[F]): EntityT[F, S, E, S] = EntityT.reader[F, S, E]

  def write(newEvents: NonEmptyChain[E])(implicit
      applicative: Applicative[F]
  ): EntityT[F, S, E, Unit] =
    EntityT.writer(newEvents)
}

object EntityT extends EntityRunFunctions {

  def writer[F[_]: Applicative, S, E](newEvents: NonEmptyChain[E]): EntityT[F, S, E, Unit] =
    new EntityT((_, existing) => write(newEvents)(existing))

  def purr[F[_]: Applicative, S, E, A](a: A): EntityT[F, S, E, A] = new EntityT((_, events) =>
    pure(a)(events)
  )

  def lifter[F[_]: Functor, S, E, A](fa: F[A]): EntityT[F, S, E, A] =
    new EntityT((_, events) => fa.map(a => (events, a).asRight))

  def reader[F[_]: Monad, S, E]: EntityT[F, S, E, S] = new EntityT(read[F, S, E])

  implicit def instance[F[_], S, E](implicit
      monad0: Monad[F]
  ): EntityLift[EntityT[F, S, E, *], F, S, E] =
    new EntityTLiftInstance[F, S, E] {
      override protected implicit def monad: Monad[F] = monad0
    }
}
