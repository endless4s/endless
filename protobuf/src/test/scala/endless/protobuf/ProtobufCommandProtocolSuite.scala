package endless.protobuf
import cats.{Functor, Id}
import endless.core.protocol.{Decoder, IncomingCommand, OutgoingCommand}
import endless.protobuf.test.proto.dummy.{DummyCommand, DummyReply}
import org.scalacheck.Prop.forAll
import cats.syntax.functor._

class ProtobufCommandProtocolSuite extends munit.ScalaCheckSuite {
  test("protobuf command protocol") {
    forAll { (int: Int, str: String, reply: Boolean) =>
      val outgoingCommand = protocol.client.dummy(str, int)
      val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
      val encodedReply = incomingCommand
        .runWith((_: String, _: Int) => reply)
        .map(incomingCommand.replyEncoder.encode)
      assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }

  val protocol = new ProtobufCommandProtocol[DummyAlg] {
    override def server[F[_]]: Decoder[IncomingCommand[F, DummyAlg]] =
      ProtobufDecoder[DummyCommand].map { case DummyCommand(x, y, _) =>
        incomingCommand[F, DummyReply, Boolean](_.dummy(x, y), DummyReply(_))
      }

    override def client: DummyAlg[OutgoingCommand[*]] = (x: String, y: Int) =>
      outgoingCommand[DummyCommand, DummyReply, Boolean](DummyCommand(x, y), _.ok)
  }

  trait DummyAlg[F[_]] {
    def dummy(x: String, y: Int): F[Boolean]
  }

}
