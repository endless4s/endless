package endless.core.interpret

import endless.core.entity.Effector
import endless.core.interpret.EffectorT.EffectorT

/** Interprets a function `F[Unit]` describing side-effects using `Effector` in context `F` with
  * `EffectorT`
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
      effector: Effector[EffectorT[F, S, Alg, *], S, Alg],
      repositoryAlg: RepositoryAlg[F],
      entityAlg: Alg[F]
  ): F[EffectorT[F, S, Alg, Unit]]
}
