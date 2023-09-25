package endless.core.interpret

import endless.core.entity.Entity

/** Interprets an algebra `Alg` expressed using `Entity` in context `F` with `EntityT`
  *
  * @tparam F
  *   effect type
  * @tparam S
  *   state
  * @tparam E
  *   event
  * @tparam Alg
  *   entity algebra
  * @return
  *   interpreted entity algebra in context `F`
  */
trait EntityInterpreter[F[_], S, E, Alg[_[_]]] {
  def apply(entity: Entity[EntityT[F, S, E, *], S, E]): F[Alg[EntityT[F, S, E, *]]]
}
