package endless.scodec
import cats.Id
import cats.syntax.functor._
import endless.core.protocol.{Decoder, IncomingCommand, OutgoingCommand}
import org.scalacheck.Prop.forAll
import scodec.Codec
import scodec.codecs.implicits._

class ScodecCommandProtocolSuite extends munit.ScalaCheckSuite {
  test("scodec command protocol") {
    forAll { (int: Int, str: String, reply: Boolean) =>
      val outgoingCommand = protocol.client.dummy(int, str)
      val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
      val encodedReply = incomingCommand
        .runWith((_: Int, _: String) => reply)
        .map(incomingCommand.replyEncoder.encode)
      assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }

  val protocol = new ScodecCommandProtocol[DummyAlg] {
    def server[F[_]]: Decoder[IncomingCommand[F, DummyAlg]] =
      ScodecDecoder(DummyCommand.scodecDecoder).map { case DummyCommand(x, y) =>
        incomingCommand[F, Boolean](_.dummy(x, y))
      }

    def client: DummyAlg[OutgoingCommand[*]] = (x: Int, y: String) =>
      outgoingCommand[DummyCommand, Boolean](DummyCommand(x, y))
  }

  trait DummyAlg[F[_]] {
    def dummy(x: Int, y: String): F[Boolean]
  }

  case class DummyCommand(x: Int, y: String)
  object DummyCommand {
    implicit val scodecDecoder: scodec.Decoder[DummyCommand] = Codec[DummyCommand].asDecoder
    implicit val scodecEncoder: scodec.Encoder[DummyCommand] = Codec[DummyCommand].asEncoder
  }
}
