package endless.core.typeclass.protocol

trait CommandProtocol[Alg[_[_]]] {
  def server[F[_]]: Decoder[IncomingCommand[F, Alg]]
  def client: Alg[OutgoingCommand[*]]
}
