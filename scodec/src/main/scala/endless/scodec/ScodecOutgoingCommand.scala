package endless.scodec

import endless.core.protocol.{Decoder, OutgoingCommand}

final class ScodecOutgoingCommand[C, +R: scodec.Decoder](command: C)(implicit
    commandEncoder: scodec.Encoder[C]
) extends OutgoingCommand[R] {
  override def payload: Array[Byte] = ScodecEncoder[C].encode(command)
  override def replyDecoder: Decoder[R] = ScodecDecoder[R]
}
