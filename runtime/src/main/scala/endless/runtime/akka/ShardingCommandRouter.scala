package endless.runtime.akka

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.util.Timeout
import cats.effect.kernel.Async
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.~>
import endless.core.typeclass.entity.EntityNameProvider
import endless.core.typeclass.protocol.{CommandRouter, EntityIDEncoder, OutgoingCommand}
import endless.runtime.akka.data.{Command, Reply}

private[akka] final class ShardingCommandRouter[F[_], ID](implicit
    sharding: ClusterSharding,
    actorSystem: ActorSystem[_],
    askTimeout: Timeout,
    idEncoder: EntityIDEncoder[ID],
    nameProvider: EntityNameProvider[ID],
    F: Async[F]
) extends CommandRouter[F, ID] {
  def routerForID(id: ID): OutgoingCommand[*] ~> F =
    new (OutgoingCommand[*] ~> F) {
      def apply[A](fa: OutgoingCommand[A]): F[A] = {
        F.fromFuture {
          F.delay {
            sharding.entityRefFor(
              EntityTypeKey[Command](nameProvider()),
              idEncoder(id)
            ) ? Command(idEncoder(id), fa.payload)
          }
        } >>= { case Reply(payload) => fa.replyDecoder.decode(payload).pure[F] }
      }
    }
}

object ShardingCommandRouter {
  implicit def apply[F[_], ID](implicit
      sharding: ClusterSharding,
      actorSystem: ActorSystem[_],
      askTimeout: Timeout,
      idEncoder: EntityIDEncoder[ID],
      nameProvider: EntityNameProvider[ID],
      F: Async[F]
  ): CommandRouter[F, ID] = new ShardingCommandRouter
}
