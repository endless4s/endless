# Durable entity

```scala
trait StateReader[F[_], S] {
  def read: F[S]
}
trait StateWriter[F[_], S] {
  def write(s: S): F[Unit]
  def modify(f: S => S): F[Unit]
  def modifyF(f: S => F[S]): F[Unit]
}
trait DurableEntity[F[_], S]
    extends StateReader[F, S]
    with StateWriter[F, S]
```

@scaladoc[DurableEntity](endless.core.entity.DurableEntity) is parametrized with entity state `S`. It is a typeclass which represents reader-writer capabilities for `F` with direct state persistence semantics, i.e., the ability to store the full state after processing each command instead of a sequence of events.

For some use cases, e.g. in scenarios with high-frequency updates where each individual update doesn't carry valuable meaning to the domain, events aren't relevant and flashing the full state is preferable.

Such "durable" entities still benefit from sharding, passivation, rebalancing, automatic recovery etc. while keeping the persistence model simple. In effect, this is a way to implement CRUD-like distributed entities while retaining the advantages of actors, such as the ability to actively schedule side-effects, precise state-machine semantics, consistent distributed in-memory state, etc.   

@@@ Note
`DurableEntity` is the equivalent of [`Stateful`](https://typelevel.org/cats-mtl/mtl-classes/stateful.html) in cats MTL, with the additional persistence semantics
@@@

@@@ tip { title="Event-sourcing" }
For the equivalent abstraction with event-sourcing abilities, see @ref:[DurableEntity](entity.md)
@@@