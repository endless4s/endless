package akka.persistence.tagless.core.typeclass

trait Decoder[Code, A] {
  def decode(encoded: Encoded[Code, A]): A
}
