package endless.core.typeclass.protocol

/** Encode an entity ID into a string
  * @tparam ID
  *   entity id
  */
trait EntityIDEncoder[-ID] {
  def encode(id: ID): String
}
