package endless.core.data

import cats.Foldable
import cats.syntax.foldable._
import endless.core.typeclass.event.EventApplier
import endless.\/

final case class EventsFolder[S, E](state: S, applier: EventApplier[S, E]) {
  def applyOnFoldable[G[_]: Foldable](foldable: G[E]): String \/ S =
    foldable.foldM(state)(applier.apply)
}
