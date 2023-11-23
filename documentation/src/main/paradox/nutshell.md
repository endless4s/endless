# In a nutshell

*Endless* bridges the gap between [Cats Effect](https://typelevel.org/cats-effect/) and [Pekko/Akka Cluster Sharding](https://pekko.apache.org/docs/pekko/current/typed/cluster-sharding.html). It makes it possible to define abstract repository and entity algebras in tagless-final style to describe distributed event sourced domains seamlessly within functional codebases. Writing applications using endless's abstraction layer also makes it easy to switch between actor frameworks. 

Implementation of the various aspects of an entity can be done using endless [*typeclasses*](https://en.wikipedia.org/wiki/Type_class) and abstractions:

 - @ref:[Sharding](sharding.md): represents the ability to interact with a specific entity in the cluster
 - @ref:[Entity](entity.md): ability to read entity state, write events and produce a reply (direct state persistence is supported with @ref:[DurableEntity](durable-entity.md))
 - @ref:[EventApplier](applier.md): description of how to [*fold*](https://en.wikipedia.org/wiki/Fold_\(higher-order_function\)) events over entity state
 - @ref:[CommandProtocol](protocol.md): ability to translate entity algebra invocations into serializable commands and replies
 - @ref:[CommandSender](sender.md): ability to deliver a command to its target entity, and provide the reply back to the sender (transport layer)
 - @ref:[Effector](effector.md): ability to produce side effects after event persistence, including control of entity passivation and chaining further interactions with the entity itself

The provided Pekko/Akka runtimes are a thin layer over Cluster Sharding. They rely exclusively on the usual public APIs (the Persistence DSL in particular) and allow for full customization of the entity behavior if lower-level tweaks are required.  

@@@ note { title="Genericity" }
Abstractions defined here are universal and could also be operated with a different event-sourcing framework or even a custom implementation. At the moment, built-in support for both Pekko and Akka is provided. A native implementation for Kubernetes is also in the works.
@@@

@@@ note { .tip title="For more info" }
Check out the blog article [Functional event-sourcing with cats-effect](https://jonas-chapuis.medium.com/functional-event-sourcing-with-akka-and-cats-7c075939fbdc)
@@@