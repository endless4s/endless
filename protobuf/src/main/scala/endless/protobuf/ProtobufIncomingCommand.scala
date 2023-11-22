package endless.protobuf

import endless.core.protocol.{Encoder, IncomingCommand}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class ProtobufIncomingCommand[F[_], R <: GeneratedMessage: GeneratedMessageCompanion, A, Alg[_[_]]](
    run: Alg[F] => F[A],
    replyContramapper: A => R
) extends IncomingCommand[F, Alg] {
  override type Reply = A
  override def runWith(alg: Alg[F]): F[A] = run(alg)
  override def replyEncoder: Encoder[A] = ProtobufEncoder[R].contramap(replyContramapper)
}
