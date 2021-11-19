package endless.core.typeclass.entity
import cats.{Applicative, Monad}
import cats.syntax.flatMap._

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
