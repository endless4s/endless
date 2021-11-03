# CommandRouter

```scala
trait CommandRouter[F[_], ID] {
  def routerForID(id: ID): OutgoingCommand[*] ~> F
}
```

@scaladoc[CommandRouter](endless.core.typeclass.protocol.CommandRouter) represents the ability to deliver a command to its target entity. It provides a natural transformation for an entity ID type, that can map the entity algebra interpreted by `CommandProtocol.client` into a context `OutgoingCommand[*]` back to `F` (precisely by sending the command and retrieving the response).

There is a built-in implementation for Akka Cluster Sharding: @github[ShardingCommandRouter](/example/src/main/scala/endless/runtime/ShardingCommandRouter.scala)

In order to support natural transformations, a @link:[cats-tagless](https://typelevel.org/cats-tagless/)  { open=new } @link:[FunctorK](https://typelevel.org/cats-tagless/typeclasses.html)  { open=new } instance must be provided for the entity algebra. This is easy to achieve thanks to built-in derivation macros:

```scala
implicit lazy val functorKInstance: cats.tagless.FunctorK[BookingAlg] = cats.tagless.Derive.functorK[BookingAlg]
```
