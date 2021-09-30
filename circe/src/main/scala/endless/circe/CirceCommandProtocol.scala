package endless.circe

import endless.core.typeclass.protocol.{CommandProtocol, Decoder, IncomingCommand, OutgoingCommand}
import endless.core.typeclass.protocol._

trait CirceCommandProtocol[Alg[_[_]]] extends CommandProtocol[Alg] {
  override def server[F[_]]: Decoder[IncomingCommand[F, Alg]]

  override def client: Alg[OutgoingCommand[*]]

  protected def outgoingCommand[C: io.circe.Encoder, R: io.circe.Decoder](
      command: C
  ): OutgoingCommand[R] = CirceOutgoingCommand(command)

  protected def incomingCommand[F[_], R: io.circe.Encoder](
      run: Alg[F] => F[R]
  ): IncomingCommand[F, Alg] = CirceIncomingCommand(run)
}
