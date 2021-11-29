package endless.scodec

import endless.core.protocol.IncomingCommand

abstract class ScodecIncomingCommand[F[_], Alg[_[_]], R: scodec.Encoder]
    extends IncomingCommand[F, Alg] {
  type Reply = R
  override def replyEncoder: ScodecEncoder[Reply] = implicitly
}

object ScodecIncomingCommand {
  def apply[F[_], Alg[_[_]], R: scodec.Encoder](
      run: Alg[F] => F[R]
  ): ScodecIncomingCommand[F, Alg, R] =
    new ScodecIncomingCommand[F, Alg, R] {
      def runWith(alg: Alg[F]): F[R] = run(alg)
    }
}
