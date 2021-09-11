package akka.persistence.tagless.core.interpret

import cats.Monad
import cats.conversions.all._
import cats.data.NonEmptyChain
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._

trait EntityTLiftInstance[F[_], S, E] extends EntityLift[EntityT[F, S, E, *], F, S, E] {
  protected implicit def monad: Monad[F]
  override def liftF[A](fa: F[A]): EntityT[F, S, E, A] = EntityT.lifter(fa)

  override def read: EntityT[F, S, E, S] = EntityT.reader[F, S, E]

  def pure[A](a: A): EntityT[F, S, E, A] = EntityT.purr(a)

  override def write(event: E, other: E*): EntityT[F, S, E, Unit] =
    EntityT.writer(NonEmptyChain(event, other: _*))

  override def flatMap[A, B](fa: EntityT[F, S, E, A])(
      f: A => EntityT[F, S, E, B]
  ): EntityT[F, S, E, B] = fa.flatMap(f)

  def tailRecM[A, B](a: A)(f: A => EntityT[F, S, E, Either[A, B]]): EntityT[F, S, E, B] =
    new EntityT[F, S, E, B]((folder, events) =>
      monad.tailRecM((events, a)) { case (events, a) =>
        f(a).runAcc(folder, events).flatMap {
          case Right((nextEvents, Left(nextA))) =>
            f(nextA).runAcc(folder, nextEvents).map {
              case Right((nextNextEvents, Left(a))) => (nextNextEvents, a).asLeft // we keep digging
              case Right((nextNextEvents, Right(b))) => (nextNextEvents, b).asRight.asRight
              case Left(invalidFoldReason)           => invalidFoldReason.asLeft.asRight
            }
          case Right((nextEvents, Right(b))) => (nextEvents, b).asRight.asRight.pure
          case Left(invalidFoldReason)       => invalidFoldReason.asLeft.asRight.pure
        }
      }
    )

}
