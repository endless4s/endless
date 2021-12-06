package endless.protobuf

import endless.core.protocol.Decoder
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

object ProtobufDecoder {
  def apply[A <: GeneratedMessage: GeneratedMessageCompanion]: Decoder[A] =
    (payload: Array[Byte]) => implicitly[GeneratedMessageCompanion[A]].parseFrom(payload)
}
