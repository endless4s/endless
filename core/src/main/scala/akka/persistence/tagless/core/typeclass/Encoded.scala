package akka.persistence.tagless.core.typeclass

trait Encoded[Code, A] {
  def payload: Code
  def decoder: Decoder[Code, A]
}
