package akka.persistence.tagless.core.data

import cats.Foldable
import akka.persistence.tagless.\/
import akka.persistence.tagless.core.typeclass.EventApplier
import cats.syntax.foldable._

final case class EventsFolder[S, E](state: S, applier: EventApplier[S, E]) {
  def applyOnFoldable[G[_]: Foldable](foldable: G[E]): String \/ S =
    foldable.foldM(state)(applier.apply)
}
