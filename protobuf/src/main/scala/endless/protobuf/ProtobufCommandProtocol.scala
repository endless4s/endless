package endless.protobuf

import endless.core.protocol.{CommandProtocol, Decoder, Encoder, IncomingCommand, OutgoingCommand}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

trait ProtobufCommandProtocol[Alg[_[_]]] extends CommandProtocol[Alg] {
  protected def outgoingCommand[
      C <: GeneratedMessage: GeneratedMessageCompanion,
      R <: GeneratedMessage: GeneratedMessageCompanion,
      A
  ](command: C, replyMapper: R => A): OutgoingCommand[A] = new OutgoingCommand[A] {
    override def payload: Array[Byte] = ProtobufEncoder[C].encode(command)
    override def replyDecoder: Decoder[A] = ProtobufDecoder[R].map(replyMapper)
  }

  protected def incomingCommand[F[_], R <: GeneratedMessage: GeneratedMessageCompanion, A](
      run: Alg[F] => F[A],
      replyContramapper: A => R
  ): IncomingCommand[F, Alg] = new IncomingCommand[F, Alg] {
    override type Reply = A
    override def runWith(alg: Alg[F]): F[A] = run(alg)
    override def replyEncoder: Encoder[A] = ProtobufEncoder[R].contramap(replyContramapper)
  }
}
