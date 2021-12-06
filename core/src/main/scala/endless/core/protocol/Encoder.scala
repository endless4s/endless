package endless.core.protocol

/** Generic binary encoder
  * @tparam A
  *   value
  */
trait Encoder[-A] {

  /** Encode value of type `A` into a binary array
    * @param a
    *   value
    * @return
    *   corresponding byte array
    */
  def encode(a: A): Array[Byte]

  /** Converts this encoder to a `Encoder[B]` using the supplied B => A */
  def contramap[B](f: B => A): Encoder[B] = (b: B) => encode(f(b))
}
