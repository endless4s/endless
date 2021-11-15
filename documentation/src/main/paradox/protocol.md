# CommandProtocol

```scala
trait CommandProtocol[Alg[_[_]]] {
  def server[F[_]]: Decoder[IncomingCommand[F, Alg]]
  def client: Alg[OutgoingCommand[*]]
}
```

@scaladoc[CommandProtocol](endless.core.typeclass.protocol.CommandProtocol) is to be implemented for each entity algebra. It provides a `client` interpretation wrapping each function into a @scaladoc[OutgoingCommand](endless.core.typeclass.protocol.OutgoingCommand) context and a `server` decoder which can deserialize an incoming command into @scaladoc[IncomingCommand](endless.core.typeclass.protocol.IncomingCommand).

`OutgoingCommand` is able to encode the command into a binary representation ready to be sent over the wire and also decode the expected subsequent reply. `IncomingCommand` is able to decode the incoming command, invoke the corresponding handler and encode the reply.

In other words, `client` materializes algebra invocations into concrete serializable outgoing commands and `server` acts as a switchboard for incoming commands. See @github[BookingCommandProtocol](/example/src/main/scala/endless/example/protocol/BookingCommandProtocol.scala) for a concrete example.

@@@ note { .info title="Explicit or implicit representations" }
`CommandProtocol` is the entry point for implementations to map algebra entries to concrete commands and replies. We tend to prefer explicit materialization for migration safety but nothing prevents protocol implementers to opt for automatic serialization via macros. 
We provide helpers for definition of binary protocols in `endless-scodec-helpers` and JSON protocols in `endless-circe-helpers`.     
@@@