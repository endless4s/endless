package akka.persistence.tagless.core.typeclass

trait CommandMessage[Alg[_[_]], Code] {
  type Reply
  def command: Command[Alg, Reply]
  def replyEncoder: Encoder[Reply, Code]
}
