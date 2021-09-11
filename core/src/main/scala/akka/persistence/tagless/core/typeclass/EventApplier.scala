package akka.persistence.tagless.core.typeclass

import akka.persistence.tagless.\/

trait EventApplier[S, E] extends ((S, E) => String \/ S) {
  def apply(state: S, event: E): String \/ S
}
