package endless.runtime.akka.data

import akka.actor.typed.ActorRef

@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
final case class Command(id: String, payload: Array[Byte])(val replyTo: ActorRef[Reply])
