package endless.scodec

import endless.core.protocol.Decoder
import endless.scodec.ScodecDecoder.DecodingException
import scodec.bits.BitVector

class ScodecDecoder[+A](implicit decoder: scodec.Decoder[A]) extends Decoder[A] {
  override def decode(payload: Array[Byte]): A = decoder
    .decode(BitVector(payload))
    .fold(
      error => throw DecodingException(error.messageWithContext),
      _.value
    )
}

object ScodecDecoder {
  final case class DecodingException(message: String) extends RuntimeException(message)

  implicit def apply[A: scodec.Decoder]: ScodecDecoder[A] = new ScodecDecoder[A]
}
