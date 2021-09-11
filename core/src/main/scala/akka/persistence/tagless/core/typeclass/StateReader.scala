package akka.persistence.tagless.core.typeclass

trait StateReader[F[_], S] {
  def read: F[S]
}
