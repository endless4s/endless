# endless <img src="https://github.com/endless4s/endless/blob/master/logo/symbol-only/transparent/1x/logo%20endless.png" width="50">
[![Release](https://github.com/endless4s/endless/actions/workflows/release.yml/badge.svg?branch=master)](https://github.com/endless4s/endless/actions/workflows/release.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.endless4s/endless-core_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.endless4s/endless-core_2.13)
[![codecov](https://codecov.io/gh/endless4s/endless/branch/master/graph/badge.svg?token=aH9vOhLxVS)](https://codecov.io/gh/endless4s/endless)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

endless is a Scala library to describe event sourced entities using tagless-final algebras, running with built-in implementations for Akka. 

(*endless* refers to persistence and the ever flowing stream of events capturing state evolution with ES, and is a nod to *tag-less*)    

## Work in progress 🚧
This project is a work in progress...

## Algebras
In a nutshell, the library allows describing event sourcing logic with pure functional algebras. This makes it possible to code Akka cluster applications using cats-effect, from beginning to end in tagless-final style, with a pure algebraic domain.

## Typeclasses
The library essentially provides `Entity` & `Repository` type classes that can be used to describe entity behavior within an abstract effectful context `F`:

### Repository
```scala
trait Repository[F[_], ID, Alg[_[_]]] {
  def entityFor(id: ID): Alg[F]
}
```
`Repository` is parametrized with the entity ID type `ID` and the entity algebra `Alg[_[_]]` and represents obtaining an instance of that algebra (the entity) for a specific ID.

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

```scala
trait BookingRepositoryAlg[F[_]] {
  def bookingFor(bookingID: BookingID): BookingAlg[F]
}

trait BookingAlg[F[_]] {
  def place(bookingID: BookingID,
            passengerCount: Int,
            origin: LatLon,
            destination: LatLon): F[BookingAlreadyExists \/ Unit]
  def get: F[BookingUnknown.type \/ Booking]
  def changeOrigin(newOrigin: LatLon): F[BookingUnknown.type \/ Unit]
  def changeDestination(newDestination: LatLon): F[BookingUnknown.type \/ Unit]
  def changeOriginAndDestination(newOrigin: LatLon,
                                 newDestination: LatLon
                                ): F[BookingUnknown.type \/ Unit]
}
```

Implementation of the repository algebra is trivial using `Repository`:
```scala
final case class BookingRepository[F[_]: Monad](repository: Repository[F, BookingID, BookingAlg])
  extends BookingRepositoryAlg[F] {
  import repository._
  def bookingFor(bookingID: BookingID): BookingAlg[F] = entityFor(bookingID)
}
```

Implementation of entity behavior is done using the `Entity` DSL:

```scala
final case class BookingEntity[F[_]: Monad](entity: Entity[F, Option[Booking], BookingEvent])
    extends BookingAlg[F] {
  import entity._

  def place(
      bookingID: BookingID,
      passengerCount: Int,
      origin: LatLon,
      destination: LatLon
  ): F[BookingAlreadyExists \/ Unit] =
    read >>= {
      case Some(_) => BookingAlreadyExists(bookingID).asLeft.pure
      case None =>
        write(BookingPlaced(bookingID, origin, destination, passengerCount))
          .map(_.asRight)
    }

  def get: F[BookingUnknown.type \/ Booking] = ifKnown(_.pure)

  def changeOrigin(newOrigin: LatLon): F[BookingUnknown.type \/ Unit] =
    ifKnown(booking =>
      if (booking.origin =!= newOrigin) entity.write(OriginChanged(newOrigin)) else ().pure
    )

  def changeDestination(newDestination: LatLon): F[BookingUnknown.type \/ Unit] =
    ifKnown(booking =>
      if (booking.destination =!= newDestination) entity.write(DestinationChanged(newDestination))
      else ().pure
    )

  def changeOriginAndDestination(
      newOrigin: LatLon,
      newDestination: LatLon
  ): F[BookingUnknown.type \/ Unit] = changeOrigin(newOrigin) >> changeDestination(newDestination)

  private def ifKnown[A](fa: Booking => F[A]): F[BookingUnknown.type \/ A] =
    read >>= {
      case Some(booking) => fa(booking).map(_.asRight)
      case None          => BookingUnknown.asLeft.pure
    }
}
```

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
This example application can be found in `endless-example` and can be run directly: `sbt run` 

## Inspiration
This library takes most of its inspiration from the outstanding work done by Denis Mikhaylov & contributors on https://github.com/notxcain/aecor as well as the excellent [blog series](https://pavkin.ru/aecor-intro/) by Vladimir Pavkin.
Compared to Aecor, this library aims to be smaller in scale (no projections, at least for now), stick close to DDD & Akka nomenclature and provide a thin runtime layer delegating to native Akka persistence as much as possible.    
