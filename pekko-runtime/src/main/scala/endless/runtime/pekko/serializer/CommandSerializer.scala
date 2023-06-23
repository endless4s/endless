package endless.runtime.pekko.serializer

import org.apache.pekko.actor.typed.ActorRefResolver
import org.apache.pekko.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import org.apache.pekko.serialization.{BaseSerializer, SerializerWithStringManifest}
import cats.syntax.show._
import com.google.protobuf.ByteString
import endless.runtime.pekko.data.Command
import endless.runtime.pekko.serializer.CommandSerializer.ManifestKey

import java.io.NotSerializableException

/** Internal command carrier serializer, configured in reference.conf
  * @param system
  *   actor system
  */
class CommandSerializer(val system: org.apache.pekko.actor.ExtendedActorSystem)
    extends SerializerWithStringManifest
    with BaseSerializer {
  private implicit val actorRefResolver: ActorRefResolver = ActorRefResolver(system.toTyped)

  def manifest(o: AnyRef): String = o match {
    case _: Command => ManifestKey
    case _ =>
      throw new IllegalArgumentException(
        show"Can't serialize object of type ${o.getClass.getName} in [${getClass.getName}]"
      )
  }

  def toBinary(o: AnyRef): Array[Byte] = o match {
    case command: Command =>
      proto.command
        .Command(
          command.id,
          ByteString.copyFrom(command.payload),
          actorRefResolver.toSerializationFormat(command.replyTo)
        )
        .toByteArray
    case _ =>
      throw new IllegalArgumentException(s"Cannot serialize object of type [${o.getClass.getName}]")
  }

  def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case ManifestKey =>
      val protoCommand = proto.command.Command.parseFrom(bytes)
      Command(protoCommand.id, protoCommand.payload.toByteArray)(
        actorRefResolver.resolveActorRef(protoCommand.replyTo)
      )
    case _ =>
      throw new NotSerializableException(
        s"Unimplemented deserialization of message with manifest [$manifest] in [${getClass.getName}]"
      )
  }
}

object CommandSerializer {
  val ManifestKey: String = "Command"
}
