package endless.protobuf

import endless.core.protocol.Encoder
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

object ProtobufEncoder {
  implicit def apply[A <: GeneratedMessage: GeneratedMessageCompanion]: Encoder[A] =
    (a: A) => implicitly[GeneratedMessageCompanion[A]].toByteArray(a)
}
