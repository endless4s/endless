package akka.persistence.tagless.core.typeclass

trait Repository[F[_], ID, Alg[_[_]]] {
  def entityFor(id: ID): Alg[F]
}
