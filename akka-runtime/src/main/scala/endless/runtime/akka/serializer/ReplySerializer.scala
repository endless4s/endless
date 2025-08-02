package endless.runtime.akka.serializer

import akka.serialization.{BaseSerializer, SerializerWithStringManifest}
import endless.runtime.akka.data.Reply
import endless.runtime.akka.serializer.ReplySerializer.ManifestKey

import java.io.NotSerializableException

/** Internal reply carrier serializer, configured in reference.conf
  * @param system
  *   actor system
  */
class ReplySerializer(val system: akka.actor.ExtendedActorSystem)
    extends SerializerWithStringManifest
    with BaseSerializer {
  def manifest(o: AnyRef): String = o match {
    case _: Reply => ManifestKey
    case _        =>
      throw new IllegalArgumentException(
        s"Can't serialize object of type ${o.getClass.getName} in [${getClass.getName}]"
      )
  }

  def toBinary(o: AnyRef): Array[Byte] = o match {
    case reply: Reply => reply.payload
    case _            =>
      throw new IllegalArgumentException(s"Cannot serialize object of type [${o.getClass.getName}]")
  }

  def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case ManifestKey => Reply(bytes)
    case _           =>
      throw new NotSerializableException(
        s"Unimplemented deserialization of message with manifest [$manifest] in [${getClass.getName}]"
      )
  }
}

object ReplySerializer {
  val ManifestKey: String = "Reply"
}
