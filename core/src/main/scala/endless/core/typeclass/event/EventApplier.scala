package endless.core.typeclass.event

import endless.\/

/** Function that defines transition of the state given an event (or invalid event for the given
  * state)
  * @tparam S
  *   state
  * @tparam E
  *   event
  */
trait EventApplier[S, E] extends ((Option[S], E) => String \/ Option[S])
