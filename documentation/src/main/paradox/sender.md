# CommandSender

```scala
trait CommandSender[F[_], ID] {
  def senderForID(id: ID): OutgoingCommand[*] ~> F
}
```

@scaladoc[CommandSender](endless.core.protocol.CommandSender) represents the ability to deliver a command to its target entity. It provides a natural transformation of `OutgoingCommand` types "back to" `F`, where the "transformation" is typically the flow of encoding/decoding and delivering command/responses in the cluster ultimately leading back to a `F[A]` reply value. 

In other words, it implements the transport layer of the cluster, and is typically provided by the runtime.

The built-in implementations in Pekko and Akka runtimes simply rely on an actor `ask`: @github[ShardingCommandSender](/runtime/src/main/scala/endless/runtime/pekko/ShardingCommandSender.scala)