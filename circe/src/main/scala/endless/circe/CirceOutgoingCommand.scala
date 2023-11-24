package endless.circe

import endless.core.protocol.{Decoder, OutgoingCommand}

final class CirceOutgoingCommand[C, +R: io.circe.Decoder](command: C)(implicit
    commandEncoder: io.circe.Encoder[C]
) extends OutgoingCommand[R] {
  def payload: Array[Byte] = CirceEncoder[C].encode(command)
  def replyDecoder: Decoder[R] = CirceDecoder[R]
}
