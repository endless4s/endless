package endless.core.event

import endless.\/

/** Function that defines transition of the state given an event (or invalid event for the given
  * state).
  *
  * @note
  *   returning `None` allows ignoring irrelevant events before entity is created
  * @tparam S
  *   state
  * @tparam E
  *   event
  */
trait EventApplier[S, E] extends ((Option[S], E) => String \/ Option[S])
