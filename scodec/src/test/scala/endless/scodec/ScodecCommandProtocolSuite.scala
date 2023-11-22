package endless.scodec
import cats.Id
import endless.core.protocol.{CommandSender, Decoder, IncomingCommand}
import org.scalacheck.Prop.forAll
import scodec.Codec
import scodec.codecs.implicits._

class ScodecCommandProtocolSuite extends munit.ScalaCheckSuite {
  test("scodec command protocol") {
    forAll { (int: Int, str: String, reply: Boolean, id: String) =>
      implicit val sender: CommandSender[Id, ID] = localCommandSenderWith(reply)
      val actual = dummyProtocol.clientFor(id).dummy(int, str)
      assertEquals(actual, reply)
    }
  }

  val dummyProtocol = new ScodecCommandProtocol[String, DummyAlg] {
    def server[F[_]]: Decoder[IncomingCommand[F, DummyAlg]] =
      ScodecDecoder(DummyCommand.scodecDecoder).map { case DummyCommand(x, y) =>
        handleCommand[F, Boolean](_.dummy(x, y))
      }

    def clientFor[F[_]](id: ID)(implicit sender: CommandSender[F, ID]): DummyAlg[F] =
      (x: Int, y: String) => sendCommand[F, DummyCommand, Boolean](id, DummyCommand(x, y))
  }

  trait DummyAlg[F[_]] {
    def dummy(x: Int, y: String): F[Boolean]
  }

  def localCommandSenderWith(reply: Boolean): CommandSender[Id, ID] = CommandSender.local(
    dummyProtocol,
    new DummyAlg[Id] {
      def dummy(x: Int, y: String): Id[Boolean] = reply
    }
  )

  case class DummyCommand(x: Int, y: String)
  object DummyCommand {
    implicit val scodecDecoder: scodec.Decoder[DummyCommand] = Codec[DummyCommand].asDecoder
    implicit val scodecEncoder: scodec.Encoder[DummyCommand] = Codec[DummyCommand].asEncoder
  }
  type ID = String

}
