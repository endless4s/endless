# Effector

```scala
trait StateReader[F[_], S] {
  def read: F[Option[S]]
}
trait Passivator[F[_]] {
  def enablePassivation(after: FiniteDuration = Duration.Zero): F[Unit]
  def disablePassivation: F[Unit]
}
trait Self[F[_], Alg[_[_]]] {
  def self: F[Alg[F]]
}
trait Effector[F[_], S] extends StateReader[F, S] with Passivator[F] with Self[F]
```

@scaladoc[Effector](endless.core.typeclass.entity.Effector) is used to describe side effects occurring **after** event persistence and entity recovery.

Side-effects are typically asynchronous operations such as kafka writes, outgoing REST requests, and [entity passivation](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html#passivation) (flushing out of memory). `Effector` is used in a `Effector => F[Unit]` function provided upon entity deployment (e.g. @github[BookingEffector](/example/src/main/scala/endless/example/logic/BookingEffector.scala)). In the provided Akka runtime, the resulting `F[Unit]` is executed in *run & forget* mode so that command reply is not delayed by any lengthy side-effect (`Self` can be used to notify success or failure of asynchronous operations back to the entity).

## State-derived side-effects
@scaladoc[StateReader](endless.core.typeclass.entity.StateReader) allows reading the updated entity state after event persistence or recovery. 

## Passivation
@scaladoc[Passivator](endless.core.typeclass.entity.Passivator) allow fine grain control over passivation. In certain domains, entities can evolve into "dormant" states (e.g. after a `BookingCancelled` event) for which it is beneficial to trigger passivation, either immediately or after a certain delay. This enables proactive optimization of cluster resources.

## Self & process definition
@scaladoc[Self](endless.core.typeclass.entity.Self) exposes the algebra of the entity within the effector context. This allows definition of asynchronous processes that involve interaction with the very same entity, typically to define entities acting as [process managers](https://www.infoq.com/news/2017/07/process-managers-event-flows/) (see below for more detail).  

@@@ note { .tip title="At least once delivery with zero latency" }
For most processes, *at least once* delivery guarantees are required. This can be achieved with a projection, however at the cost of some incurred latency. Actual latency depends on the database and event journal implementation used, as well as the projection throughput. One must also make sure to distribute the projection across the cluster to avoid creating a central choke point. Even so, if a projector process gets stalled for some reason, this can create a cascade effect with events pending processing building up. 

An effective alternative to using a projection is to track process completion in the entity state itself. Launching asynchronous operations directly as a side-effect of an event has zero latency overhead and also the added advantage that the process launches within the node of the entity which triggered it, thus benefiting from inherent distribution. 

By enabling [*remember-entities*](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html#remembering-entities), we can achieve guaranteed *at-least-once* completion of asynchronous processes thanks to effector running right after recovery (thus withstanding node crash or shard rebalancing).

*endless* makes it easy to implement this pattern with `Self`. Here's the recipe, as illustrated in the example application @github[example](/example/src/main/scala/endless/example/logic/BookingEffector.scala):

1. `BookingPlaced` event gets persisted. At this point, entity state represents pending acceptation of the booking `Booking(..., status = Pending)`
2. Effector function inspects the state, and in case of `Pending` status, asks a third-party service for availability and notifies the entity of the result:
  
```scala
val availabilityProcess: Booking => F[Unit] = booking =>
      booking.status match {
        case Status.Pending =>
          for {
            isAvailable <- availabilityAlg.isCapacityAvailable(booking.time, booking.passengerCount)
            entity <- self
            _ <- entity.notifyCapacity(isAvailable)
          } yield ()
        case _ => ().pure
      }
```

3.`BookingAccepted` or `BookingRejected` events are persisted and entity state is updated accordingly.

@@@
