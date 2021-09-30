package endless.runtime.akka.serializer

import akka.serialization.{BaseSerializer, SerializerWithStringManifest}
import endless.runtime.akka.data.Command
import endless.runtime.akka.serializer.CommandSerializer.ManifestKey
import endless.runtime.akka.serializer.proto
import cats.syntax.show._
import com.google.protobuf.ByteString

import java.io.NotSerializableException

class CommandSerializer(val system: akka.actor.ExtendedActorSystem)
    extends SerializerWithStringManifest
    with BaseSerializer {
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
        .Command(command.id, ByteString.copyFrom(command.payload))
        .toByteArray
    case _ =>
      throw new IllegalArgumentException(s"Cannot serialize object of type [${o.getClass.getName}]")
  }

  def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case ManifestKey => proto.command.Command.parseFrom(bytes)
    case _ =>
      throw new NotSerializableException(
        s"Unimplemented deserialization of message with manifest [$manifest] in [${getClass.getName}]"
      )
  }
}

object CommandSerializer {
  val ManifestKey: String = "Command"
}
