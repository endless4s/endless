package endless.core.typeclass.protocol

trait IncomingCommand[F[_], Alg[_[_]]] {
  type Reply
  def runWith(alg: Alg[F]): F[Reply]
  def replyEncoder: Encoder[Reply]
}
