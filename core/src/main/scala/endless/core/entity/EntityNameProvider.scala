package endless.core.entity

/** Function returning a name for an entity kind
  * @tparam ID
  *   entity identifier type, used here to identify the entity kind
  */
trait EntityNameProvider[ID] extends (() => String)
