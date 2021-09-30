package endless.core.typeclass.entity

trait Repository[F[_], ID, Alg[_[_]]] {
  def entityFor(id: ID): Alg[F]
}
