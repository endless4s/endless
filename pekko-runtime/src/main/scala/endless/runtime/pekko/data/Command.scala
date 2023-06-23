package endless.runtime.pekko.data

import org.apache.pekko.actor.typed.ActorRef

/** Internal carrier data type for entity commands
  * @param id
  *   target entity ID
  * @param payload
  *   binary payload
  * @param replyTo
  *   actor reference where to deliver the reply
  */
@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
final case class Command(id: String, payload: Array[Byte])(
    val replyTo: ActorRef[Reply]
)
