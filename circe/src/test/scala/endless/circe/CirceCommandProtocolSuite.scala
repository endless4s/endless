package endless.circe
import cats.Id
import endless.core.protocol.{CommandSender, Decoder, IncomingCommand}
import io.circe.generic.auto._
import org.scalacheck.Prop.forAll

class CirceCommandProtocolSuite extends munit.ScalaCheckSuite {
  test("circe command protocol") {
    forAll { (int: Int, str: String, reply: Boolean, id: ID) =>
      implicit val sender: CommandSender[Id, ID] = localCommandSenderWith(reply)
      val actual = dummyProtocol.clientFor(id).dummy(int, str)
      assertEquals(actual, reply)
    }
  }

  val dummyProtocol = new CirceCommandProtocol[ID, DummyAlg] {
    def server[F[_]]: Decoder[IncomingCommand[F, DummyAlg]] =
      CirceDecoder[DummyCommand].map { case DummyCommand(x, y) =>
        handleCommand[F, Boolean](_.dummy(x, y))
      }

    def clientFor[F[_]](id: ID)(implicit sender: CommandSender[F, ID]): DummyAlg[F] =
      (x: Int, y: String) => sendCommand[F, DummyCommand, Boolean](id, DummyCommand(x, y))
  }

  def localCommandSenderWith(reply: Boolean): CommandSender[Id, ID] = CommandSender.local(
    dummyProtocol,
    new DummyAlg[Id] {
      def dummy(x: Int, y: String): Id[Boolean] = reply
    }
  )

  trait DummyAlg[F[_]] {
    def dummy(x: Int, y: String): F[Boolean]
  }
  case class DummyCommand(x: Int, y: String)
  type ID = String
}
