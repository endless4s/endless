package endless.core.protocol

import cats.{Functor, ~>}
import cats.syntax.functor.*

/** `CommandSender[F, ID]` provides a natural transformation to deliver an outgoing command to where
  * the entity resides and decode the reply as a simple value in the `F` context.
  * @tparam F
  *   context
  * @tparam ID
  *   entity ID
  */
trait CommandSender[F[_], ID] {

  /** Returns a natural transformation to deliver an outgoing command to the entity with this
    * particular ID.
    * @param id
    *   entity ID
    */
  def senderForID(id: ID): OutgoingCommand[*] ~> F
}

object CommandSender {

  /** Local command sender, for testing purposes
    * @param protocol
    *   command protocol, used for its server component
    * @param alg
    *   entity algebra instance
    * @tparam F
    *   context
    * @tparam ID
    *   entity ID
    * @tparam Alg
    *   entity algebra
    * @return
    *   a command sender that runs the command locally
    */
  def local[F[_]: Functor, ID, Alg[_[_]]](
      protocol: CommandProtocol[ID, Alg],
      alg: Alg[F]
  ): CommandSender[F, ID] = (_: ID) =>
    new (OutgoingCommand ~> F) {
      def apply[A](fa: OutgoingCommand[A]): F[A] = {
        val incoming = protocol.server[F].decode(fa.payload)
        incoming.runWith(alg).map(incoming.replyEncoder.encode).map(fa.replyDecoder.decode)
      }
    }
}
