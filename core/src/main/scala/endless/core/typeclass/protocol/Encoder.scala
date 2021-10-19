package endless.core.typeclass.protocol

/** Function to encode a value of type `A` into binary array
  */
trait Encoder[-A] extends (A => Array[Byte])
