package endless.circe.serializer

import akka.serialization.SerializerWithStringManifest
import cats.syntax.eq._
import cats.syntax.show._
import io.circe.parser.parse
import io.circe.{Decoder, Encoder, Json}

import java.io.NotSerializableException
import java.nio.charset.{Charset, StandardCharsets}
import scala.reflect.ClassTag

abstract class CirceSerializer[T <: AnyRef: ClassTag] extends SerializerWithStringManifest {
  // Pick a unique identifier for your Serializer,
  // you've got a couple of billions to choose from,
  // 0 - 40 is reserved by Akka itself
  def identifier: Int

  private val charset = StandardCharsets.UTF_8

  lazy val className: String = implicitly[ClassTag[T]].runtimeClass.getName

  override def manifest(o: AnyRef): String = className

  def encoder: Encoder[T]
  def decoder: Decoder[T]

  override def toBinary(obj: AnyRef): Array[Byte] =
    obj match {
      case value: T => encoder(value).noSpaces.getBytes(charset)
      case other =>
        throw new IllegalArgumentException(
          show"Unknown type of object ${other.getClass.getName}, expected $className"
        )
    }

  def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case manifest if manifest === className =>
        parseFromJson(bytes, charset, decoder)
      case manifest =>
        throw new IllegalArgumentException(show"Unknown manifest $manifest expected $className")
    }

  private def parseFromJson(bytes: Array[Byte], charset: Charset, decoder: Decoder[T]): T = {
    val jsonString = new String(bytes, charset)
    parse(jsonString) match {
      case Right(json) => decodeJson(json, decoder)
      case Left(error) =>
        throw new NotSerializableException(
          show"Unable to parse json string as Json: $error '$jsonString'"
        )
    }
  }

  private def decodeJson(json: Json, decoder: Decoder[T]): T =
    decoder.decodeJson(json) match {
      case Right(value) => value
      case Left(error) =>
        throw new NotSerializableException(
          show"Unable to decode json to ${implicitly[ClassTag[T]].runtimeClass.getSimpleName}: $error '${json.noSpaces}')"
        )
    }
}
