package endless.circe

import endless.core.typeclass.protocol.Encoder

import java.nio.charset.StandardCharsets

class CirceEncoder[-A](implicit encoder: io.circe.Encoder[A]) extends Encoder[A] {
  def encode(a: A): Array[Byte] = encoder.apply(a).noSpaces.getBytes(StandardCharsets.UTF_8)
}

object CirceEncoder {
  implicit def apply[A: io.circe.Encoder]: CirceEncoder[A] = new CirceEncoder[A]
}
