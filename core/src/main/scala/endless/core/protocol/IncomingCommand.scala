package endless.core.protocol

/** Represents an incoming entity command. Embeds the `Reply` type, the ability to run it on the
  * entity algebra in `F` context and specifies the encoder to be used to encode the reply.
  * @tparam F
  *   context
  * @tparam Alg
  *   entity algebra
  */
trait IncomingCommand[F[_], Alg[_[_]]] {
  type Reply
  def runWith(alg: Alg[F]): F[Reply]
  def replyEncoder: Encoder[Reply]
}
