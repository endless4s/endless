package endless.core.typeclass.protocol

trait Encoder[-A] {
  def encode(a: A): Array[Byte]
}
