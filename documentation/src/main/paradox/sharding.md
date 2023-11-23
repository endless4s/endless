# Sharding

```scala
trait Sharding[F[_], ID, Alg[_[_]]] {
  def entityFor(id: ID): Alg[F]
}
```

@scaladoc[Sharding](endless.core.entity.Sharding) is parametrized with entity ID type `ID` and entity algebra `Alg[_[_]]`. It represents the ability to obtain an instance of that algebra (the entity) for a specific ID. This operation represents location-transparent access to sharded entities in a distributed system. It is made available to the interpreter of the repository algebra (e.g. @github[ShardedBookings](/example/src/main/scala/endless/example/logic/ShardedBookings.scala)). 
