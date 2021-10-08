package endless.core.typeclass.effect

trait Effector[F[_]] {
  def afterPersist: F[Unit]
}
