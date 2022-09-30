# In a nutshell

*Endless* bridges the gap between [Cats Effect](https://typelevel.org/cats-effect/) and [Akka Cluster Sharding](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html). It makes it possible to define abstract repository and entity algebras in tagless-final style to describe distributed event sourced domains seamlessly within functional codebases. 

Implementations for various aspects of an entity are provided via abstract interpreters making use of endless [*typeclasses*](https://en.wikipedia.org/wiki/Type_class):

 - @ref:[Repository](repository.md): represents the ability to interact with a specific entity in the cluster
 - @ref:[Entity](entity.md): ability to process a command by reading the state, writing events affecting the state and producing a reply
 - @ref:[EventApplier](applier.md): ability to [*fold*](https://en.wikipedia.org/wiki/Fold_\(higher-order_function\)) events over entity state
 - @ref:[CommandProtocol](protocol.md): ability to translate entity algebra invocations into serializable commands and replies
 - @ref:[Effector](effector.md): ability to produce side effects after event persistence, including passivation of the entity itself

The provided Akka runtime is a thin layer over Akka Cluster Sharding and relies exclusively on Akka's public API. It simply makes use of Akka Persistence entity behavior DSL behind the scenes, while allowing for full customization of the entity behavior if lower-level tweaks are required.  

@@@ note { title="Genericity" }
Abstractions defined here are universal and could also be operated with a different event-sourcing framework or even a custom implementation. At the moment, built-in support for Akka is provided only.
@@@

@@@ note { .tip title="For more info" }
Check out the blog article [Functional event-sourcing with cats-effect](https://jonas-chapuis.medium.com/functional-event-sourcing-with-akka-and-cats-7c075939fbdc)
@@@