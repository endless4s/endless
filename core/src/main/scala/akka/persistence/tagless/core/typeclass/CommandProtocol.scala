package akka.persistence.tagless.core.typeclass

trait CommandProtocol[Alg[_[_]], Code] {
  def server: Decoder[CommandMessage[Alg, Code], Code]
  def client: Alg[Encoded[Code, *]]
}
