package endless.core.interpret

import cats.Applicative
import endless.core.entity.SideEffect

/** Interprets a function `F[Unit]` describing a side-effect using `Effector` in context `F`
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
trait SideEffectInterpreter[F[_], S, Alg[_[_]], RepositoryAlg[_[_]]] {
  def apply(repository: RepositoryAlg[F], alg: Alg[F]): F[SideEffect[F, S, Alg]]
}

object SideEffectInterpreter {

  /** Lifts a pure interpreter into an `SideEffectInterpreter` (which is expressed in context `F`)
    * @param pureSideEffect
    *   pure side-effect (i.e. it doesn't require any effect to create the side-effect instance)
    * @tparam F
    *   effect type
    * @tparam S
    *   state
    * @tparam Alg
    *   entity algebra
    * @tparam RepositoryAlg
    *   repository algebra
    * @return
    *   side-effect interpreter in context `F`
    */
  def lift[F[_]: Applicative, S, Alg[_[_]], RepositoryAlg[_[_]]](
      pureSideEffect: (RepositoryAlg[F], Alg[F]) => SideEffect[F, S, Alg]
  ): SideEffectInterpreter[F, S, Alg, RepositoryAlg] =
    (repository: RepositoryAlg[F], alg: Alg[F]) =>
      Applicative[F].pure(pureSideEffect(repository, alg))

  /** Creates a no-op side-effect interpreter, for repositories that don't require any side-effect
    * @tparam F
    *   effect type
    * @tparam S
    *   state
    * @tparam Alg
    *   entity algebra
    * @tparam RepositoryAlg
    *   repository algebra
    * @return
    *   unit side-effect interpreter in context `F`
    */
  def unit[F[_]: Applicative, S, Alg[_[_]], RepositoryAlg[_[_]]]
      : SideEffectInterpreter[F, S, Alg, RepositoryAlg] = lift((_, _) => _ => Applicative[F].unit)
}
