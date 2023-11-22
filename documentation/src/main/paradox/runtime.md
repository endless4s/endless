# Pekko and Akka runtimes

Once required interpreters and typeclass instances have been defined, deploying an entity with Pekko or Akka boils down to a single call to @scaladoc[deployRepository](endless.core.entity.Deployer). This requires an actor system and the cluster sharding extension in implicit scope, bundled in the type @scaladoc[PekkoCluster](endless.runtime.pekko.deploy.PekkoCluster) (or @scaladoc[AkkaCluster](endless.runtime.akka.deploy.AkkaCluster)). The recommended pattern is to use the built-in @scaladoc[managedResource](endless.runtime.akka.deploy.AkkaCluster.managedResource) helper method to obtain an instance of this class, which wraps actor system creation and shutdown with a @link:[Resource](https://typelevel.org/cats-effect/docs/std/resource) { open=new }.  

## How to initiate the distributed cluster
The entrypoint is available by importing `endless.runtime.pekko.syntax.deploy.*` or `endless.runtime.akka.syntax.deploy.*` and calling `deployRepository` with the required parameters. 

This function ties everything together and delivers a cats effect @link:[Resource](https://typelevel.org/cats-effect/docs/std/resource) { open=new } with an instance of [DeployedPekkoRepository](endless.runtime.pekko.deploy.DeployedPekkoRepository) in context `F` bundling the `RepositoryAlg` instance together with the ref to the shard region actor returned by the call to Pekko's @link:[ClusterSharding.init](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html#basic-example) { open=new }.

The following snippet is the scaffolding for the library's sample application (in its Pekko form), a simple API to manage vehicles and bookings:

@@snip [ExampleApp](/example/src/main/scala/endless/example/app/pekko/PekkoApp.scala) { #main }

`deployRepository` is parameterized with the context `F` and the various involved types: `S` for  entity state, `E` for events, `ID` for entity ID and `Alg` & `RepositoryAlg` for entity and repository algebras respectively (both higher-kinded type constructors).

In order to bridge Pekko/Akka's implicit asynchronicity with the side-effect free context `F` used for algebras, it requires @link:[Async](https://typelevel.org/cats-effect/docs/typeclasses/async) { open=new } from `F`. This makes it possible to use the @link:[Dispatcher](https://typelevel.org/cats-effect/docs/std/dispatcher) { open=new } mechanism for running the command handling monadic chain synchronously from within the actor thread.

`Logger` from @link:[`log4cats`](https://github.com/typelevel/log4cats) { open=new } is also required as the library supports basic logging capabilities.

@@@ warning { title="Important" }
`deployRepository` needs to be called upon application startup, before joining the cluster as the `ClusterSharding` extension needs to know about the various entity types beforehand.
@@@

@@@ note { title="Durable entity" }
 The equivalent method for durable entities is @scaladoc[deployDurableRepository](endless.core.entity.DurableDeployer).
@@@

## Internals

### Protocol
Thanks to the @ref:[CommandProtocol](protocol.md) instance, entity algebra calls can be "materialized" into concrete commands and replies. These types are encoded to a binary payload, which is transported within an internal protobuf envelope @github:[command.proto](/runtime/src/main/protobuf/command.proto).

[ShardingCommandSender](/runtime/src/main/scala/endless/runtime/pekko/ShardingCommandSender.scala) takes care of delivering the commands to the right entity and returning the reply simply by using Pekko/Akka's `ask`.

### Deployer
Internally, @github[deployRepository](/runtime/src/main/scala/endless/runtime/pekko/PekkoDeployer.scala) uses @link:[EventSourcedBehavior](https://pekko.apache.org/docs/pekko/current/typed/persistence.html#example-and-core-api) { open=new } DSL to configure the entity in the following way (for @github[deployDurableRepository](/runtime/src/main/scala/endless/runtime/pekko/DurablePekkoDeployer.scala), this is [DurableStateBehavior](https://pekko.apache.org/docs/pekko/current/typed/durable-state/persistence.html#cluster-sharding-and-durablestatebehavior)):

#### Command handler

 1. use @ref:[CommandProtocol.server](protocol.md) to decode the command and invoke the corresponding algebra logic, interpreted internally with @scaladoc[EntityT](endless.core.interpret.EntityT). Interpretation of the monadic chain occurs, leading to one or more events and a return value. 
 2. hand in produced events to Pekko/Akka's `Effect.persist`.
 3. trigger any side effects in @ref:[Effector](effector.md) with a run-and-forget using `Dispatcher` from within `thenRun`
 4. encode the reply and feed it into `thenReply`
 
#### Event handler 
This is simply a synchronous run of @ref:[EventApplier](applier.md) (using `Dispatcher`). Left values are translated into thrown exceptions as Pekko/Akka doesn't give us other means to deal with event handling errors.

#### Recovery
Upon successful recovery, we log an info entry and run the effector, while we log a warning entry upon recovery failure.

@@@ note { .tip title="Custom behavior" } 
The built-in behavior is further customizable via a `customizeBehavior` function parameter that can be optionally passed into [PekkoDeploymentParameters](endless.runtime.pekko.deploy.PekkoDeployer.PekkoDeploymentParameters) or [AkkaDeploymentParameters](endless.runtime.akka.deploy.AkkaDeployer.AkkaDeploymentParameters). 
@@@

@@@ warning { title="Compatibility" }
Since Akka/Pekko [does not allow mixed versions](https://doc.akka.io/docs/akka/current/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed) in a project, Akka/Pekko dependencies of `endless-runtime-akka` and `endless-runtime-pekko` respectively are marked a `Provided`. This means that your application `libraryDependencies` needs to directly include Akka or Pekko as a dependency. The minimal supported Akka version is $akka.min.version$, and Pekko version is $pekko.min.version$.  
@@@