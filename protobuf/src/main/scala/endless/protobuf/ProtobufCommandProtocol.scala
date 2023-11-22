package endless.protobuf

import endless.core.protocol.{CommandProtocol, CommandSender, IncomingCommand}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

trait ProtobufCommandProtocol[ID, Alg[_[_]]] extends CommandProtocol[ID, Alg] {
  protected def sendCommand[F[
      _
  ], C <: GeneratedMessage: GeneratedMessageCompanion, R <: GeneratedMessage: GeneratedMessageCompanion, A](
      id: ID,
      command: C,
      replyMapper: R => A
  )(implicit sender: CommandSender[F, ID]): F[A] =
    CommandProtocol.sendCommand(id, new ProtobufOutgoingCommand[C, R, A](command, replyMapper))

  protected def handleCommand[F[_], R <: GeneratedMessage: GeneratedMessageCompanion, A](
      run: Alg[F] => F[A],
      replyContramapper: A => R
  ): IncomingCommand[F, Alg] = new ProtobufIncomingCommand(run, replyContramapper)
}
