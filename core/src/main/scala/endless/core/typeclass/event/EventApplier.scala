package endless.core.typeclass.event

import endless.\/

trait EventApplier[S, E] extends ((S, E) => String \/ S) {
  def apply(state: S, event: E): String \/ S
}
