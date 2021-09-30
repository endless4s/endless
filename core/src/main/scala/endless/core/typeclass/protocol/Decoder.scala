package endless.core.typeclass.protocol

trait Decoder[+A] {
  def decode(payload: Array[Byte]): A
}
