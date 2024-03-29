# CommandProtocol

```scala
trait CommandProtocol[Alg[_[_]]] {
  def server[F[_]]: Decoder[IncomingCommand[F, Alg]]
  def clientFor[F[_]](id: ID)(implicit sender: CommandSender[F, ID]): Alg[F]
}
```

@scaladoc[CommandProtocol](endless.core.protocol.CommandProtocol) is to be implemented for each entity algebra. It provides an RPC-like `client` in charge of transforming calls into instances of @scaladoc[OutgoingCommand](endless.core.protocol.OutgoingCommand), delivering such commands to the targeted entity using a @scaladoc[CommandSender](endless.core.protocol.CommandSender) and decoding the reply. It also defines the corresponding `server`, which can decode commands into @scaladoc[IncomingCommand](endless.core.protocol.IncomingCommand) instances, run the corresponding entity logic and encode the reply.

`OutgoingCommand` is able to encode the command into a binary representation ready to be sent over the wire and also decode the expected subsequent reply. `IncomingCommand` is able to decode the incoming command, invoke the corresponding handler and encode the reply.

In other words, `clientFor` materializes algebra invocations into outgoing commands delivered to `server`, which acts as a switchboard for incoming commands. See @github[BookingCommandProtocol](/example/src/main/scala/endless/example/protocol/BookingCommandProtocol.scala) for a concrete example.

@@@ note { .info title="Explicit or implicit representations" }
`CommandProtocol` is the entry point for implementations to map algebra entries to concrete commands and replies. Having these lower-level aspects described separately makes it easier to have precise control of versions and to deal with migration challenges. 

We provide helpers for definition of binary protocols in `endless-protobuf-helpers` as well as `endless-scodec-helpers` and JSON protocols in `endless-circe-helpers`.

Definition of protobuf protocols is very convenient using `endless-protobuf-helpers` because scalaPB-generated types can be referenced directly, there is no need for a separate representation of commands (and associated data-mapping headaches).
@@@

@@@ note { .tip title="Testing" }
Command protocols can be tested in isolation via synchronous round-trip exercise of the journey _client invocation -> command materialization -> command encoding -> command decoding -> behavior invocation -> reply materialization -> reply encoding -> reply decoding_. See @github[BookingCommandProtocolSuite](/example/src/test/scala/endless/example/protocol/BookingCommandProtocolSuite.scala) for an example.
@@@
