# Entity

```scala
trait StateReader[F[_], S] {
  def read: F[S]
}
trait EventWriter[F[_], E] {
  def write(event: E, other: E*): F[Unit]
}
trait Entity[F[_], S, E] extends StateReader[F, S] with EventWriter[F, E]
```

@scaladoc[Entity](endless.core.entity.Entity) is parametrized with entity state `S` and events `E`. It is a typeclass which represents reader-writer capabilities for `F` with event-sourcing semantics, i.e. the abilities to read current entity state from the context and persist events. `Entity` is typically used by the entity algebra command handling interpreter (e.g. @github[BookingEntity](/example/src/main/scala/endless/example/logic/BookingEntity.scala)). 

## Functional event sourcing
*Reader-writer* is a natural fit for describing event sourcing behavior: the monadic chain represents event sequencing and corresponding evolution of the state (see also [here](https://pavkin.ru/aecor-part-2/) and [here](https://www.youtube.com/watch?v=kDkRRkkVlxQ)).

@@@ Note
`StateReader` is the equivalent of [`Ask`](https://typelevel.org/cats-mtl/mtl-classes/ask.html) in cats MTL, and similarly  `EventWriter` is the equivalent of [`Tell`](https://typelevel.org/cats-mtl/mtl-classes/tell.html).
@@@

Advantages of this abstraction are:

- the command is the call and the reply is simply the final resulting value in the monadic chain, there are no explicit representations
- maximal composability since it's just `flatMap` all the way down, making it easier to work with a reduced set of events
- `read` always provides the up-to-date state and event folding happens transparently behind the scenes
- pure & side-effect free logic that is easy to test

@@@ warning { title="Responsivity" }
When defining event-sourced entities, it is considered best practice to process commands and formulate a reply quickly as it makes the system responsive. Long-running processes should only initiate as the result of events, which also has the added benefit that they can be restored upon recovery or be driven by a projection. See @ref:[Effector](effector.md) to find out how to describe side effects with *endless*.  
@@@

@@@ tip { title="Event-less persistence" }
It is also possible to benefit from cluster sharding without involving event-sourcing, see @ref:[DurableEntity](durable-entity.md)
@@@

@@@ note { title="About performance" }
When composing a sequence of computations which has multiple *writes* with interspersed *reads*, the state is folded before each read by the interpreter. This is necessary to provide a constant version of the state during interpretation.

This is an operation that the runtime (Akka) will also do behind the scenes when evolving the entity state. Redundant invocations of the folding function can therefore occur with the elevated monadic abstraction. 

However, in most cases this overhead is insignificant:

- event application needs to be as fast and simple as possible as it is of course invoked repeatedly for each event during recovery
- any possible redundant folds only happens upon reception of a command, not upon recovery
- command handling behavior with multiple interspersed reads and writes is less frequent than the more common read-then-write pattern
@@@
