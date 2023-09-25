package endless.core.interpret

import endless.core.entity.DurableEntity
import endless.core.interpret.DurableEntityT.DurableEntityT

/** Interprets an algebra `Alg` expressed using `DurableEntity` in context `F` with `DurableEntityT`
  *
  * @tparam F
  *   effect type
  * @tparam S
  *   state
  * @tparam Alg
  *   entity algebra
  * @return
  *   interpreted entity algebra in context `F`
  */
trait DurableEntityInterpreter[F[_], S, Alg[_[_]]] {
  def apply(entity: DurableEntity[DurableEntityT[F, S, *], S]): F[Alg[DurableEntityT[F, S, *]]]
}
