package endless.scodec

import endless.core.protocol.Encoder

class ScodecEncoder[-A](implicit encoder: scodec.Encoder[A]) extends Encoder[A] {
  override def encode(a: A): Array[Byte] = encoder.encode(a).require.toByteArray
}

object ScodecEncoder {
  implicit def apply[A: scodec.Encoder]: ScodecEncoder[A] = new ScodecEncoder[A]
}
