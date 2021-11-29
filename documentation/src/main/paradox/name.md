# EntityNameProvider

```scala
trait EntityNameProvider[ID] extends (() => String)
```

@scaladoc[EntityNameProvider](endless.core.entity.EntityNameProvider) simply provides the name (type name, or kind) of the entity, for a specific entity ID type. This maps to Akka's @link:[EntityTypeKey.name](https://doc.akka.io/japi/akka/current/akka/cluster/sharding/typed/scaladsl/EntityTypeKey.html#name()) { open=new } 