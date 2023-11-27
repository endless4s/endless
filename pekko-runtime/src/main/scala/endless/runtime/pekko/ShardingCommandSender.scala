package endless.runtime.pekko

import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import org.apache.pekko.util.Timeout
import cats.effect.kernel.Async
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.show.*
import cats.~>
import endless.core.entity.EntityNameProvider
import endless.core.protocol.{CommandSender, EntityIDEncoder, OutgoingCommand}
import endless.runtime.pekko.data.{Command, Reply}
import org.typelevel.log4cats.Logger

/** Implementation of [[CommandSender]] for pekko cluster sharding
  *
  * Retrieves the entity ref and asks the command, then decodes the reply and lifts it into `F`
  *
  * @param sharding
  *   pekko cluster sharding extension
  * @param askTimeout
  *   pekko ask timeout
  * @param idEncoder
  *   entity ID encoder
  * @param nameProvider
  *   entity name provider
  * @tparam F
  *   context, supports [[Async]] for bridging with `Future`
  * @tparam ID
  *   entity ID
  */
private[pekko] final class ShardingCommandSender[F[_]: Logger, ID](implicit
    sharding: ClusterSharding,
    askTimeout: Timeout,
    idEncoder: EntityIDEncoder[ID],
    nameProvider: EntityNameProvider[ID],
    F: Async[F]
) extends CommandSender[F, ID] {
  def senderForID(id: ID): OutgoingCommand[*] ~> F =
    new (OutgoingCommand[*] ~> F) {
      def apply[A](fa: OutgoingCommand[A]): F[A] = {
        val encodedID = idEncoder.encode(id)
        F.fromFuture {
          Logger[F].debug(
            show"Sending command to ${nameProvider()} entity $encodedID"
          ) >> F
            .delay {
              sharding.entityRefFor(
                EntityTypeKey[Command](nameProvider()),
                encodedID
              ) ? Command(encodedID, fa.payload)
            }
        } >>= { case Reply(payload) =>
          Logger[F].debug(
            show"Got reply from ${nameProvider()} entity $encodedID"
          ) >> fa.replyDecoder.decode(payload).pure[F]
        }
      }
    }
}

object ShardingCommandSender {
  implicit def apply[F[_]: Logger, ID](implicit
      sharding: ClusterSharding,
      askTimeout: Timeout,
      idEncoder: EntityIDEncoder[ID],
      nameProvider: EntityNameProvider[ID],
      F: Async[F]
  ): CommandSender[F, ID] = new ShardingCommandSender
}
