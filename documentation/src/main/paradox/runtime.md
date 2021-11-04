# Akka runtime

Once required interpreters and typeclass instances have been defined, deploying an entity with Akka boils down to a single call to @scaladoc[deployEntity](endless.runtime.akka.Deployer). This naturally requires an actor system and the cluster sharding extension in implicit scope.

## `deployEntity`
This function brings everything together and delivers a cats effect @link:[Resource](https://typelevel.org/cats-effect/docs/std/resource) { open=new } with the repository instance in context `F` bundled with the ref to the shard region actor returned by the call to Akka's @link:[ClusterSharding.init](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html#basic-example) { open=new }.

The following snippet is the scaffolding for the library's sample application, a simple API to manage bookings:

@@snip [ExampleApp](/example/src/main/scala/endless/example/ExampleApp.scala) { #main }

`deployEntity` is parameterized with the context `F` and the various involved types: `S` for  entity state, `E` for events, `ID` for entity ID and `Alg` & `RepositoryAlg` for entity and repository algebras respectively (both higher-kinded type constructors).

Entity algebra `Alg` must also be equipped with an instance of `FunctorK` to support natural transformations and @ref:[CommandRouter](router.md).   

In order to bridge Akka's implicit asynchronicity with the side-effect free context `F` used for algebras, it requires @link:[Async](https://typelevel.org/cats-effect/docs/typeclasses/async) { open=new } from `F`. This makes it possible to use the @link:[Dispatcher](https://typelevel.org/cats-effect/docs/std/dispatcher) { open=new } mechanism for running the command handling monadic chain synchronously from within the actor thread.

`Logger` from @link:[`log4cats`](https://github.com/typelevel/log4cats) { open=new } is also required as the library supports basic logging capabilities.

@@@ warning { title="Important" }
`deployEntity` needs to be called upon application startup, before joining the cluster as the `ClusterSharding` extension needs to know about the various entity types beforehand.
@@@

## Internals

### Protocol
Thanks to the @ref:[CommandProtocol](protocol.md) instance, entity algebra calls can be "materialized" into concrete commands and replies which are carried in an internal protobuf binary format @github:[command.proto](/runtime/src/main/protobuf/command.proto).
[ShardingCommandRouter](/runtime/src/main/scala/endless/runtime/akka/ShardingCommandRouter.scala) takes care of delivering the commands to the right entity and returning the reply simply by using Akka's `ask`.

### Deployer
Internally, @github[deployEntity](/runtime/src/main/scala/endless/runtime/akka/Deployer.scala) uses Akka @link:[EventSourcedBehavior](https://doc.akka.io/docs/akka/current/typed/persistence.html#example-and-core-api) { open=new } DSL to configure the entity in the following way:

#### Command handler

 1. use @ref:[CommandProtocol.server](protocol.md) to decode the command and invoke the corresponding algebra logic, interpreted internally with @scaladoc[EntityT](endless.core.interpret.EntityT). Interpretation of the monadic chain occurs, leading to one or more events and a return value. 
 2. hand in produced events to Akka's `Effect.persist`.
 3. trigger any side effects in @ref:[Effector](effector.md) by interpreting it with @scaladoc[EffectorT](endless.core.interpret.EffectorT) and running it synchronously with `Dispatcher` from within `thenRun`
 4. encode the reply and feed it into `thenReply`
 
#### Event handler 
This is simply a synchronous run of @ref:[EventApplier](applier.md) (using `Dispatcher`). Left values are translated into thrown exceptions as Akka doesn't give us other means to deal with event handling errors.

#### Recovery
Upon successful recovery, we log an info entry and run the effector, while we log a warning entry upon recovery failure.

@@@ note { .tip title="Custom behavior" } 
The built-in behavior is further customizable via a `customizeBehavior` function parameter that can be optionally passed into `deployEntity`. 
@@@