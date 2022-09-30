# EventApplier
```scala
trait EventApplier[S, E] extends ((Option[S], E) => String \/ Option[S])
```
Application of an event on the entity state (aka. *folding* the events over the state) is defined with an @scaladoc[EventApplier](endless.core.event.EventApplier) pure function, parametrized with the state `S` and events `E`. This is a tupled function of a possibly defined state and the event, leading to either a new version of the state or an error (`\/` is a type alias for `Either`).