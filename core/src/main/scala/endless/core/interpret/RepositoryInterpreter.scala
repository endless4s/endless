package endless.core.interpret

import endless.core.entity.Repository

/** Interprets an algebra `RepositoryAlg` expressed using `Repository` in context `F`
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
  def apply(repository: Repository[F, ID, Alg]): F[RepositoryAlg[F]]
}
