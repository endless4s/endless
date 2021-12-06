package endless.core.protocol

/** Generic binary decoder
  * @tparam A
  *   value
  */
trait Decoder[+A] {

  /** Decode binary array into value of type `A`
    * @param payload
    *   array of bytes
    * @return
    *   value
    */
  def decode(payload: Array[Byte]): A

  /** Converts this decoder to a Decoder[B] using the supplied A => B */
  def map[B](f: A => B): Decoder[B] = (payload: Array[Byte]) => f(decode(payload))
}
