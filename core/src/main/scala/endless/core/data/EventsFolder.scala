package endless.core.data

import cats.Foldable
import cats.syntax.foldable._
import endless.\/
import endless.core.event.EventApplier

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
final case class EventsFolder[S, E](state: Option[S], applier: EventApplier[S, E]) {
  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def applyOnFoldable[G[_]: Foldable](foldable: G[E]): String \/ Option[S] =
    foldable.foldM(state)(applier.apply)
}
