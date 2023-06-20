package endless.runtime.akka

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.util.Timeout
import cats.effect.kernel.Async
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.show._
import cats.~>
import endless.core.entity.EntityNameProvider
import endless.core.protocol.{CommandRouter, EntityIDEncoder, OutgoingCommand}
import endless.runtime.akka.data.{Command, Reply}
import org.typelevel.log4cats.Logger

/** Implementation of [[CommandRouter]] for Akka cluster sharding
  *
  * Retrieves the entity ref and asks the command, then decodes the reply and lifts it into `F`
  * @param sharding
  *   Akka cluster sharding extension
  * @param askTimeout
  *   Akka ask timeout
  * @param idEncoder
  *   entity ID encoder
  * @param nameProvider
  *   entity name provider
  * @tparam F
  *   context, supports [[Async]] for bridging with `Future`
  * @tparam ID
  *   entity ID
  */
private[akka] final class ShardingCommandRouter[F[_]: Logger, ID](implicit
    sharding: ClusterSharding,
    askTimeout: Timeout,
    idEncoder: EntityIDEncoder[ID],
    nameProvider: EntityNameProvider[ID],
    F: Async[F]
) extends CommandRouter[F, ID] {
  def routerForID(id: ID): OutgoingCommand[*] ~> F =
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

object ShardingCommandRouter {
  implicit def apply[F[_]: Logger, ID](implicit
      sharding: ClusterSharding,
      askTimeout: Timeout,
      idEncoder: EntityIDEncoder[ID],
      nameProvider: EntityNameProvider[ID],
      F: Async[F]
  ): CommandRouter[F, ID] = new ShardingCommandRouter
}
