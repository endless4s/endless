# Effector

```scala
trait StateReader[F[_], S] {
  def read: F[Option[S]]
}
trait Passivator[F[_]] {
  def enablePassivation(after: FiniteDuration = Duration.Zero): F[Unit]
  def disablePassivation: F[Unit]
}
trait Effector[F[_], S] extends StateReader[F, S] with Passivator[F]
```

@scaladoc[Effector](endless.core.typeclass.entity.Effector) is used to describe side effects occurring **after** event persistence and entity recovery. 

Side-effects are typically asynchronous operations such as kafka writes, outgoing REST requests, and [entity passivation](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html#passivation) (flushing out of memory). `Effector` is typically used in a `Effector => F[Unit]` function provided upon entity deployment (e.g. @github[BookingEffector](/example/src/main/scala/endless/example/logic/BookingEffector.scala)).

## State-derived side-effects
@scaladoc[StateReader](endless.core.typeclass.entity.StateReader) allows reading the updated entity state after event persistence or recovery. 

@@@ note { .tip title="At least once delivery with zero latency" }
For most side-effects, *at least once* delivery guarantees are required. This can be achieved with a projection, however at the cost of some incurred latency. Effective latency depends on the database and event journal implementation used, as well as the projection throughput. An effective alternative to using a projection is to track successful delivery in the entity state itself, in particular if the side-effect has some domain meaning. The following pattern can be used:

1. Event gets persisted, e.g. `BookingCreated`. Entity state is updated with a flag allowing to track side-effect completion, e.g. `BookingState(..., pendingNotification = true)`
2. Effector function triggers side-effects according to the state, e.g. `read >> (state => if (state.pendingNotification) notifyNewBooking(state))`
3. At the last stage of the asynchronous operation, successful completion is signalled back to the entity via a dedicated entry in entity algebra (and underlying command) e.g. `booking.wasNotified()`
@@@

## Passivation
@scaladoc[Passivator](endless.core.typeclass.entity.Passivator) allow fine grain control over passivation. In certain domains, entities can evolve into "dormant" states (e.g. after a `BookingCancelled` event) for which it is beneficial to trigger passivation, either immediately or after a certain delay. This enables proactive optimization of cluster resources.  
