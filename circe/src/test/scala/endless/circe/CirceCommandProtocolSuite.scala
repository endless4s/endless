package endless.circe
import cats.Id
import endless.core.typeclass.protocol.{Decoder, IncomingCommand, OutgoingCommand}
import org.scalacheck.Prop.forAll
import io.circe.generic.auto._
import cats.syntax.functor._

class CirceCommandProtocolSuite extends munit.ScalaCheckSuite {
  test("circe command protocol") {
    forAll { (int: Int, str: String, reply: Boolean) =>
      val outgoingCommand = protocol.client.dummy(int, str)
      val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
      val encodedReply = incomingCommand
        .runWith((_: Int, _: String) => reply)
        .map(incomingCommand.replyEncoder.encode)
      assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }

  val protocol = new CirceCommandProtocol[DummyAlg] {
    def server[F[_]]: Decoder[IncomingCommand[F, DummyAlg]] = CirceDecoder(
      implicitly[io.circe.Decoder[DummyCommand]].map { case DummyCommand(x, y) =>
        incomingCommand[F, Boolean](_.dummy(x, y))
      }
    )
    def client: DummyAlg[OutgoingCommand[*]] = (x: Int, y: String) =>
      outgoingCommand[DummyCommand, Boolean](DummyCommand(x, y))
  }

  trait DummyAlg[F[_]] {
    def dummy(x: Int, y: String): F[Boolean]
  }
  case class DummyCommand(x: Int, y: String)
}
