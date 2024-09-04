package endless.core.interpret

import cats.Monad
import cats.data.NonEmptyChain
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import endless.core.entity.Entity

class EntityTLiftInstance[F[_], S, E](implicit fMonad: Monad[F])
    extends Entity[EntityT[F, S, E, *], S, E]
    with Monad[EntityT[F, S, E, *]] {
  implicit lazy val monad: Monad[EntityT[F, S, E, *]] = new Monad[EntityT[F, S, E, *]] {
    def pure[A](x: A): EntityT[F, S, E, A] = EntityT.purr(x)

    def flatMap[A, B](fa: EntityT[F, S, E, A])(f: A => EntityT[F, S, E, B]): EntityT[F, S, E, B] =
      fa.flatMap(f)

    def tailRecM[A, B](a: A)(f: A => EntityT[F, S, E, Either[A, B]]): EntityT[F, S, E, B] =
      new EntityT[F, S, E, B]((folder, events) =>
        fMonad.tailRecM((events, a)) { case (events, a) =>
          f(a).runAcc(folder, events).flatMap {
            case Right((nextEvents, Left(nextA))) =>
              f(nextA).runAcc(folder, nextEvents).map {
                case Right((nextNextEvents, Left(a))) =>
                  (nextNextEvents, a).asLeft // we keep digging
                case Right((nextNextEvents, Right(b))) => (nextNextEvents, b).asRight.asRight
                case Left(invalidFoldReason)           => invalidFoldReason.asLeft.asRight
              }
            case Right((nextEvents, Right(b))) => (nextEvents, b).asRight.asRight.pure[F]
            case Left(invalidFoldReason)       => invalidFoldReason.asLeft.asRight.pure[F]
          }
        }
      )
  }

  override def read: EntityT[F, S, E, Option[S]] = EntityT.reader[F, S, E]
  override def write(event: E, other: E*): EntityT[F, S, E, Unit] =
    EntityT.writer(NonEmptyChain(event, other *))

  def pure[A](x: A): EntityT[F, S, E, A] = monad.pure(x)

  def flatMap[A, B](fa: EntityT[F, S, E, A])(f: A => EntityT[F, S, E, B]): EntityT[F, S, E, B] =
    monad.flatMap(fa)(f)

  def tailRecM[A, B](a: A)(f: A => EntityT[F, S, E, Either[A, B]]): EntityT[F, S, E, B] =
    monad.tailRecM(a)(f)
}
