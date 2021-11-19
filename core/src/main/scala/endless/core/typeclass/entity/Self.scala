package endless.core.typeclass.entity

/** Self represent the entity itself via its algebra
  * @tparam F
  *   context
  * @tparam Alg
  *   entity algebra
  */
trait Self[F[_], Alg[_[_]]] {
  def self: F[Alg[F]]
}

object Self {
  def apply[F[_], Alg[_[_]]](implicit self: Self[F, Alg]): Self[F, Alg] = self
}
