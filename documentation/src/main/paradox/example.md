# Example app

Endless example application is a small API for managing imaginary bookings for passenger trips from some origin to some destination. It can be found in `endless-example` and can be run directly: `sbt run`. 

## API
It has a simple CRUD API for those bookings:

@@snip [ExampleApp](/example/src/main/scala/endless/example/ExampleApp.scala) { #api }

## Scaffolding
The application is assembled via a call to @scaladoc[deployEntity](endless.runtime.akka.Deployer.deployEntity) (see @ref:[runtime](runtime.md) for more details)

@@snip [ExampleApp](/example/src/main/scala/endless/example/ExampleApp.scala) { #main }

## Algebras
You might have spotted the two algebra types in the snippet above, which are defined like so:

@@snip [BookingRepositoryAlg](/example/src/main/scala/endless/example/algebra/BookingRepositoryAlg.scala) { #definition }

@@snip [BookingAlg](/example/src/main/scala/endless/example/algebra/BookingAlg.scala) { #definition }

## Implementations
Implementation of the repository algebra is trivial using `Repository` typeclass instance (injected by `deployEntity`):

@@snip [BookingRepository](/example/src/main/scala/endless/example/logic/BookingRepository.scala) { #definition }

Implementation of entity algebra is done using the `Entity` typeclass instance (also injected by `deployEntity`):

@@snip [BookingEntity](/example/src/main/scala/endless/example/logic/BookingEntity.scala) { #definition }

## Event handling 

In this simple example, events essentially set fields in the state:

@@snip [BookingEventApplier](/example/src/main/scala/endless/example/logic/BookingEventApplier.scala) { #definition }

## Protocol
Command and reply encoding/decoding on client and server side is done by interpreting the entity algebra with `IncomingCommand` and `OutgoingCommand` contexts respectively:

@@snip [BookingCommandProtocol](/example/src/main/scala/endless/example/protocol/BookingCommandProtocol.scala) { #example-client }
@@snip [BookingCommandProtocol](/example/src/main/scala/endless/example/protocol/BookingCommandProtocol.scala) { #example-server }

## Side-effects
We passivate bookings immediately upon cancellation, and after an hour delay for any other interaction:

@@snip [BookingEffector](/example/src/main/scala/endless/example/logic/BookingEffector.scala) { #definition }

## Testing

Unit testing for entity algebra implementation, event handling and effector is easy thanks to the parametric nature of `F`:   

@@snip [BookingEntitySuite](/example/src/test/scala/endless/example/logic/BookingEntitySuite.scala) { #example }

@@snip [BookingEventApplierSuite](/example/src/test/scala/endless/example/logic/BookingEventApplierSuite.scala) { #example }

@@snip [BookingEffectorSuite](/example/src/test/scala/endless/example/logic/BookingEffectorSuite.scala) { #example }

`CommandProtocol` is more effectively covered via component tests as it is mostly about serialization and switchboard boilerplate.