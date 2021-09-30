package endless.circe

import CirceEncoder._
import endless.core.typeclass.protocol.IncomingCommand
import io.circe.Encoder

abstract class CirceIncomingCommand[F[_], Alg[_[_]], R: io.circe.Encoder]
    extends IncomingCommand[F, Alg] {
  type Reply = R
  override def replyEncoder: CirceEncoder[Reply] = implicitly
}

object CirceIncomingCommand {
  def apply[F[_], Alg[_[_]], R: Encoder](run: Alg[F] => F[R]): CirceIncomingCommand[F, Alg, R] =
    new CirceIncomingCommand[F, Alg, R] {
      def runWith(alg: Alg[F]): F[R] = run(alg)
    }
}
