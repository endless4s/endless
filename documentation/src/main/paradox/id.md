# EntityIDEncoder

```scala
trait EntityIDEncoder[-ID] extends (ID => String)
```

@scaladoc[EntityIDEncoder](endless.core.typeclass.protocol.EntityIDEncoder) can encode the entity ID into `String` for transmission over the wire with commands. 