package endless.protobuf
import endless.core.protocol.{Decoder, OutgoingCommand}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class ProtobufOutgoingCommand[
    C <: GeneratedMessage: GeneratedMessageCompanion,
    R <: GeneratedMessage: GeneratedMessageCompanion,
    A
](command: C, replyMapper: R => A)
    extends OutgoingCommand[A] {
  override def payload: Array[Byte] = ProtobufEncoder[C].encode(command)
  override def replyDecoder: Decoder[A] = ProtobufDecoder[R].map(replyMapper)
}
