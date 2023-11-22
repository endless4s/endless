package endless.scodec

import endless.core.protocol.{CommandProtocol, CommandSender, IncomingCommand}

trait ScodecCommandProtocol[ID, Alg[_[_]]] extends CommandProtocol[ID, Alg] {
  protected def sendCommand[F[_], C: scodec.Encoder, R: scodec.Decoder](id: ID, command: C)(implicit
      sender: CommandSender[F, ID]
  ): F[R] = CommandProtocol.sendCommand(id, new ScodecOutgoingCommand(command))

  protected def handleCommand[F[_], R: scodec.Encoder](
      run: Alg[F] => F[R]
  ): IncomingCommand[F, Alg] = ScodecIncomingCommand(run)
}
