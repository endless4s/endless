package endless.core.interpret

import cats.Applicative
import endless.core.entity.Effector

/** Interprets a function `F[Unit]` describing side-effects using `Effector` in context `F`
  *
  * @tparam F
  *   effect type
  * @tparam S
  *   state
  * @tparam Alg
  *   entity algebra
  * @tparam RepositoryAlg
  *   repository algebra
  * @return
  *   interpreted side-effect function in context `F`
  */
trait EffectorInterpreter[F[_], S, Alg[_[_]], RepositoryAlg[_[_]]] {
  def apply(
      effector: Effector[F, S, Alg],
      repositoryAlg: RepositoryAlg[F],
      entityAlg: Alg[F]
  ): F[Unit]
}

object EffectorInterpreter {
  def pure[F[_]: Applicative, S, Alg[_[_]], RepositoryAlg[_[_]]](
      f: (Effector[F, S, Alg], RepositoryAlg[F], Alg[F]) => F[Unit]
  ): F[EffectorInterpreter[F, S, Alg, RepositoryAlg]] =
    Applicative[F].pure(
      (effector: Effector[F, S, Alg], repositoryAlg: RepositoryAlg[F], entityAlg: Alg[F]) =>
        f(effector, repositoryAlg, entityAlg)
    )

  def unit[F[_]: Applicative, S, Alg[_[_]], RepositoryAlg[_[_]]]
      : F[EffectorInterpreter[F, S, Alg, RepositoryAlg]] =
    pure((_, _, _) => Applicative[F].unit)
}
