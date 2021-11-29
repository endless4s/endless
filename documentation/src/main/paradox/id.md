# EntityIDCodec

```scala
trait EntityIDEncoder[-ID] {
  def encode(id: ID): String
}
trait EntityIDDecoder[+ID] {
  def decode(id: String): ID
}
trait EntityIDCodec[ID] extends EntityIDEncoder[ID] with EntityIDDecoder[ID]
```

@scaladoc[EntityIDCodec](endless.core.protocol.EntityCodec) can encode/decode the entity ID into/from `String` for transmission over the wire together with commands. 