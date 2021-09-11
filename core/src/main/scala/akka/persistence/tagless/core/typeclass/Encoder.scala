package akka.persistence.tagless.core.typeclass

trait Encoder[A, Code] {
  def encode(a: A): Encoded[A, Code]
}
