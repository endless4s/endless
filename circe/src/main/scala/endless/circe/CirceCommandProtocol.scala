package endless.circe

import endless.core.protocol.{CommandProtocol, CommandSender, IncomingCommand}

trait CirceCommandProtocol[ID, Alg[_[_]]] extends CommandProtocol[ID, Alg] {
  protected def sendCommand[F[_], C: io.circe.Encoder, R: io.circe.Decoder](id: ID, command: C)(
      implicit sender: CommandSender[F, ID]
  ): F[R] = CommandProtocol.sendCommand(id, new CirceOutgoingCommand[C, R](command))

  protected def handleCommand[F[_], R: io.circe.Encoder](
      run: Alg[F] => F[R]
  ): IncomingCommand[F, Alg] = CirceIncomingCommand(run)
}
