package endless.circe

import endless.core.typeclass.protocol.{CommandProtocol, IncomingCommand, OutgoingCommand}

trait CirceCommandProtocol[Alg[_[_]]] extends CommandProtocol[Alg] {
  protected def outgoingCommand[C: io.circe.Encoder, R: io.circe.Decoder](
      command: C
  ): OutgoingCommand[R] = CirceOutgoingCommand(command)

  protected def incomingCommand[F[_], R: io.circe.Encoder](
      run: Alg[F] => F[R]
  ): IncomingCommand[F, Alg] = CirceIncomingCommand(run)
}
