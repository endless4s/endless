package endless.protobuf
import cats.Id
import endless.core.protocol.{CommandSender, Decoder, IncomingCommand}
import endless.protobuf.test.proto.dummy.{DummyCommand, DummyReply}
import org.scalacheck.Prop.forAll

class ProtobufCommandProtocolSuite extends munit.ScalaCheckSuite {
  test("protobuf command protocol") {
    forAll { (int: Int, str: String, reply: Boolean, id: ID) =>
      implicit val sender: CommandSender[Id, ID] = localCommandSenderWith(reply)
      val actual = dummyProtocol.clientFor(id).dummy(str, int)
      assertEquals(actual, reply)
    }
  }

  val dummyProtocol = new ProtobufCommandProtocol[ID, DummyAlg] {
    override def server[F[_]]: Decoder[IncomingCommand[F, DummyAlg]] =
      ProtobufDecoder[DummyCommand].map { case DummyCommand(x, y, _) =>
        handleCommand[F, DummyReply, Boolean](_.dummy(x, y), DummyReply(_))
      }

    def clientFor[F[_]](id: ID)(implicit sender: CommandSender[F, ID]): DummyAlg[F] =
      (x: String, y: Int) =>
        sendCommand[F, DummyCommand, DummyReply, Boolean](id, DummyCommand(x, y), _.ok)
  }

  def localCommandSenderWith(reply: Boolean): CommandSender[Id, ID] = CommandSender.local(
    dummyProtocol,
    new DummyAlg[Id] {
      def dummy(x: ID, y: Int): Id[Boolean] = reply
    }
  )

  trait DummyAlg[F[_]] {
    def dummy(x: String, y: Int): F[Boolean]
  }
  type ID = String
}
