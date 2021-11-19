package endless.core.typeclass.protocol

/** Decode a string into an entity ID
  * @tparam ID
  *   entity id
  */
trait EntityIDDecoder[+ID] {
  def decode(id: String): ID
}
