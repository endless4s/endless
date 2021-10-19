package endless.circe

import CirceDecoder.{DecodingException, ParsingException}
import io.circe.{DecodingFailure, Json, ParsingFailure}
import io.circe.parser.parse
import cats.syntax.either._
import endless.core.typeclass.protocol.Decoder

import java.nio.charset.StandardCharsets

class CirceDecoder[+A](implicit decoder: io.circe.Decoder[A]) extends Decoder[A] {
  def apply(payload: Array[Byte]): A =
    parse(new String(payload, StandardCharsets.UTF_8))
      .leftMap(new ParsingException(_))
      .flatMap(decoder.decodeJson(_).leftMap(new DecodingException(_)))
      .fold(throw _, identity)
}

object CirceDecoder {
  final class ParsingException(failure: ParsingFailure) extends RuntimeException(failure.message)
  final class DecodingException(failure: DecodingFailure) extends RuntimeException(failure.message)

  implicit def apply[A: io.circe.Decoder]: Decoder[A] = new CirceDecoder[A]
}
