# Learn

## Typeclasses
The library essentially provides `Entity` & `Repository` type classes that can be used to describe entity behavior within an abstract effectful context `F`:

### Repository

```scala
trait Repository[F[_], ID, Alg[_[_]]] {
  def entityFor(id: ID): Alg[F]
}
```

@scaladoc[Repository](endless.core.typeclass.entity.Repository) is parametrized with the entity ID type `ID` and the entity algebra `Alg[_[_]]` and represents obtaining an instance of that algebra (the entity) for a specific ID.

### Entity

```scala
trait StateReader[F[_], S] {
  def read: F[S]
}
trait EventWriter[F[_], E] {
  def write(event: E, other: E*): F[Unit]
}
trait Entity[F[_], S, E] extends StateReader[F, S] with EventWriter[F, E] with Monad[F]
```

`Entity` is parametrized with entity state `S` and events `E`. It essentially represents reader-writer monad capabilities for `F`, allowing to read the current state from the context and write events into it. This is a natural fit for describing event sourcing behavior: the monadic chain represents event sequencing and according evolution of the state (see also [here](https://pavkin.ru/aecor-part-2/) and [here](https://www.youtube.com/watch?v=kDkRRkkVlxQ)).
Notice `StateReader` is the equivalent of [`Ask`](https://typelevel.org/cats-mtl/mtl-classes/ask.html) in cats MTL, and similarly  `EventWriter` is the equivalent of [`Tell`](https://typelevel.org/cats-mtl/mtl-classes/tell.html).

Some advantages of using this reader-writer monadic abstraction for event-sourcing logic are:
- the reply is simply the final resulting value in the monadic chain, there is no explicit representation
- maximal composability, it's just `flatMap` all the way down and this makes it easier to work with a reduced set of events for instance
- `read` always provides the up-to-date state and event folding happens behind the scenes
- pure logic that is easy to test

One downside is related to the usual cost with elevated abstractions, as when composing a sequence of computations which has multiple *writes* with interspersed *reads*, the state is folded before each read by the interpreter. This is an operation that typically the runtime (Akka) will also do behind the scenes when evolving the entity state. We can therefore have more invocations of the folding function than strictly necessary (but this function is supposed to be fast and this only happens upon reception of a command, not on recovery).

### EventApplier
```scala
trait EventApplier[S, E] extends ((S, E) => String \/ S) {
  def apply(state: S, event: E): String \/ S
}
```
Application of an event on the state (aka *folding* the events) is defined with an `EventApplier` instance, parametrized with the state `S` and event `E`. This is a function of the state/event tuple leading to either a new version of the state or an error (`\/` is a type alias for `Either`).

## Example

Here's an example couple of algebras for a repository of booking entities:

@@snip [BookingRepositoryAlg](/example/src/main/scala/endless/example/algebra/BookingRepositoryAlg.scala) { #definition }

@@snip [BookingAlg](/example/src/main/scala/endless/example/algebra/BookingAlg.scala) { #definition }

Implementation of the repository algebra is trivial using `Repository`:

@@snip [BookingAlg](/example/src/main/scala/endless/example/logic/BookingRepository.scala) { #definition }

Implementation of entity behavior is done using the `Entity` DSL:

@@snip [BookingAlg](/example/src/main/scala/endless/example/logic/BookingEntity.scala) { #definition }

Command and reply encoding/decoding on client and server side is done by interpreting the entity algebra with `IncomingCommand` and `OutgoingCommand` contexts respectively:

```scala
class BookingCommandProtocol extends CirceCommandProtocol[BookingAlg] {
  override def client: BookingAlg[OutgoingCommand[*]] =
    new BookingAlg[OutgoingCommand[*]] {
      def place(
          bookingID: BookingID,
          passengerCount: Int,
          origin: LatLon,
          destination: LatLon
      ): OutgoingCommand[BookingAlreadyExists \/ Unit] =
        outgoingCommand[BookingCommand, BookingAlreadyExists \/ Unit](
          PlaceBooking(bookingID, passengerCount, origin, destination)
        )

      def get: OutgoingCommand[BookingUnknown.type \/ Booking] =
        outgoingCommand[BookingCommand, BookingUnknown.type \/ Booking](Get)

      //...
    }

  override def server[F[_]]: Decoder[IncomingCommand[F, BookingAlg]] =
    CirceDecoder(io.circe.Decoder[BookingCommand].map {
      case PlaceBooking(
            rideID: BookingID,
            passengerCount: Int,
            origin: LatLon,
            destination: LatLon
          ) =>
        incomingCommand[F, BookingAlreadyExists \/ Unit](
          _.place(rideID, passengerCount, origin, destination)
        )
      case Get => incomingCommand[F, BookingUnknown.type \/ Booking](_.get)
          
      //..
    })
}
```
