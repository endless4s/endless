package endless.runtime.akka

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.util.Timeout
import cats.{Functor, Monad}
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.tagless.FunctorK
import endless.core.interpret._
import endless.core.typeclass.entity._
import endless.core.typeclass.event.EventApplier
import endless.core.typeclass.protocol.{CommandProtocol, EntityIDEncoder}
import endless.runtime.akka.data._
import ShardingCommandRouter._
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.show._
import endless.core.interpret.RepositoryT
import org.typelevel.log4cats.Logger

trait Deployer {
  def deployEntity[F[_]: Monad: Logger, S, E, ID, Alg[_[_]]: FunctorK, RepositoryAlg[_[_]]](
      createEntity: Entity[EntityT[F, S, E, *], S, E] => Alg[EntityT[F, S, E, *]],
      createRepository: Repository[F, ID, Alg] => RepositoryAlg[F],
      emptyState: S
  )(implicit
      sharding: ClusterSharding,
      actorSystem: ActorSystem[_],
      nameProvider: EntityNameProvider[ID],
      idEncoder: EntityIDEncoder[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      askTimeout: Timeout,
      F: Async[F]
  ): Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] =
    Dispatcher[F].map { dispatcher =>
      implicit val interpretedEntityAlg: Alg[EntityT[F, S, E, *]] = createEntity(
        EntityT.instance
      )
      val entityTypeKey = EntityTypeKey[Command](nameProvider())
      val akkaEntity = akka.cluster.sharding.typed.scaladsl.Entity(
        EntityTypeKey[Command](nameProvider())
      ) { context =>
        EventSourcedBehavior.withEnforcedReplies[Command, E, S](
          PersistenceId(entityTypeKey.name, context.entityId),
          emptyState,
          commandHandler = (state, command) => {
            val incomingCommand =
              commandProtocol.server[EntityT[F, S, E, *]].decode(command.payload)
            val effect = Logger[F].debug(
              show"Handling command for ${nameProvider()} entity ${command.id}"
            ) >> RepositoryT.apply
              .runCommand(state, incomingCommand)
              .flatMap {
                case Left(error) =>
                  Logger[F].warn(error) >> Effect.unhandled[E, S].thenNoReply().pure
                case Right((events, reply)) =>
                  Effect
                    .persist(events.toList)
                    .thenReply(command.replyTo) { _: S =>
                      Reply(incomingCommand.replyEncoder.encode(reply))
                    }
                    .pure
              }
            dispatcher.unsafeRunSync(effect)
          },
          eventHandler = eventApplier.apply(_, _) match {
            case Left(error) =>
              dispatcher.unsafeRunSync(Logger[F].warn(error))
              throw new EventApplierException(error)
            case Right(newState) => newState
          }
        )
      }
      (createRepository(RepositoryT.apply[F, S, E, ID, Alg]), sharding.init(akkaEntity))
    }

  final class EventApplierException(error: String) extends RuntimeException(error)
}
