package endless.scodec

import endless.core.protocol.{CommandProtocol, IncomingCommand, OutgoingCommand}

trait ScodecCommandProtocol[Alg[_[_]]] extends CommandProtocol[Alg] {
  protected def outgoingCommand[C: scodec.Encoder, R: scodec.Decoder](
      command: C
  ): OutgoingCommand[R] = ScodecOutgoingCommand(command)

  protected def incomingCommand[F[_], R: scodec.Encoder](
      run: Alg[F] => F[R]
  ): IncomingCommand[F, Alg] = ScodecIncomingCommand(run)
}
