# EventApplier
```scala
trait EventApplier[S, E] extends ((S, E) => String \/ S) {
  def apply(state: S, event: E): String \/ S
}
```
Application of an event on the entity state (aka. *folding* the events over the state) is defined with an @scaladoc[EventApplier](endless.core.typeclass.event.EventApplier) instance, parametrized with the state `S` and events `E`. This is a function of the state/event tuple leading to either a new version of the state or an error (`\/` is a type alias for `Either`).