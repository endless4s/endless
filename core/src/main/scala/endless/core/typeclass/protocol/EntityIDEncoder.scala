package endless.core.typeclass.protocol

/** Function to encode an entity ID into a string
  * @tparam ID
  *   entity id
  */
trait EntityIDEncoder[-ID] extends (ID => String)
