package endless.core.typeclass.entity

trait StateReader[F[_], S] {
  def read: F[S]
}
