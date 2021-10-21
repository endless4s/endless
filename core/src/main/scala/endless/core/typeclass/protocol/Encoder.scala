package endless.core.typeclass.protocol

trait Encoder[-A] {

  /** Encode value of type `A` into a binary array
    * @param a
    *   value
    * @return
    *   corresponding byte array
    */
  def encode(a: A): Array[Byte]
}
