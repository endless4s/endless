package endless.core.interpret

import cats.Applicative
import cats.effect.kernel.Resource
import endless.core.entity.Sharding

/** Interpret an algebra `RepositoryAlg` expressed using `Sharding` in context `F`, materializing
  * the distributed repository
  *
  * @tparam F
  *   effect type
  * @tparam ID
  *   entity ID
  * @tparam Alg
  *   entity algebra
  * @tparam RepositoryAlg
  *   repository algebra
  * @return
  *   interpreted repository algebra in context `F`
  */
trait RepositoryInterpreter[F[_], ID, Alg[_[_]], RepositoryAlg[_[_]]] {
  def apply(sharding: Sharding[F, ID, Alg]): Resource[F, RepositoryAlg[F]]
}

object RepositoryInterpreter {

  /** Lifts a pure interpreter into an `RepositoryInterpreter` (which is expressed in context `F`)
    * @param pureInterpreter
    *   pure interpreter (i.e. it doesn't require any effect to create the algebra instance)
    * @tparam F
    *   effect type
    * @tparam ID
    *   entity ID
    * @tparam Alg
    *   entity algebra
    * @tparam RepositoryAlg
    *   repository algebra
    * @return
    *   repository algebra interpreter in context `F`
    */
  def lift[F[_]: Applicative, ID, Alg[_[_]], RepositoryAlg[_[_]]](
      pureInterpreter: Sharding[F, ID, Alg] => RepositoryAlg[F]
  ): RepositoryInterpreter[F, ID, Alg, RepositoryAlg] = (sharding: Sharding[F, ID, Alg]) =>
    Resource.pure(pureInterpreter(sharding))
}
