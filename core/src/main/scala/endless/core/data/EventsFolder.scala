package endless.core.data

import cats.Foldable
import cats.syntax.foldable._
import endless.core.typeclass.event.EventApplier
import endless.\/

/** Convenience data type bundling state and event application (folding) function
  * @param state
  *   state value
  * @param applier
  *   event application function
  * @tparam S
  *   state
  * @tparam E
  *   event
  */
final case class EventsFolder[S, E](state: S, applier: EventApplier[S, E]) {
  def applyOnFoldable[G[_]: Foldable](foldable: G[E]): String \/ S =
    foldable.foldM(state)(applier.apply)
}
