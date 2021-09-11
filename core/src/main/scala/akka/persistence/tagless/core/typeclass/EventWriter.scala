package akka.persistence.tagless.core.typeclass

trait EventWriter[F[_], E] {
  def write(event: E, other: E*): F[Unit]
}
