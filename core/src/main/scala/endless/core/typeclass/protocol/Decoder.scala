package endless.core.typeclass.protocol

/** Function to decode binary payload
  * @tparam A
  *   decoded type
  */
trait Decoder[+A] extends (Array[Byte] => A)
