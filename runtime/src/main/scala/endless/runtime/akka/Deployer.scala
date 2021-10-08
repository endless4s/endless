package endless.runtime.akka

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted, RecoveryFailed}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.util.Timeout
import cats.data.ReaderT
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.show._
import cats.tagless.FunctorK
import endless.core.interpret._
import endless.core.typeclass.effect.Effector
import endless.core.typeclass.entity._
import endless.core.typeclass.event.EventApplier
import endless.core.typeclass.protocol.{CommandProtocol, CommandRouter, EntityIDEncoder}
import endless.runtime.akka.data._
import org.typelevel.log4cats.Logger

trait Deployer {

  def deployEntity[F[_]: Async: Logger, S, E, ID, Alg[_[_]]: FunctorK, RepositoryAlg[_[
      _
  ]]](
      createEntity: Entity[EntityT[F, S, E, *], S, E] => Alg[EntityT[F, S, E, *]],
      createRepository: Repository[F, ID, Alg] => RepositoryAlg[F],
      createEffector: StateReader[ReaderT[F, S, *], S] => Effector[ReaderT[F, S, *]],
      emptyState: S,
      customizeBehavior: EventSourcedBehavior[Command, E, S] => EventSourcedBehavior[
        Command,
        E,
        S
      ] = identity[EventSourcedBehavior[Command, E, S]](_)
  )(implicit
      sharding: ClusterSharding,
      actorSystem: ActorSystem[_],
      nameProvider: EntityNameProvider[ID],
      idEncoder: EntityIDEncoder[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      askTimeout: Timeout
  ): Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] =
    new DeployEntity(
      createEntity,
      createRepository,
      createEffector,
      emptyState,
      customizeBehavior
    ).apply

  final class EventApplierException(error: String) extends RuntimeException(error)

  private class DeployEntity[F[_]: Async: Logger, S, E, ID, Alg[_[_]]: FunctorK, RepositoryAlg[_[
      _
  ]]](
      createEntity: Entity[EntityT[F, S, E, *], S, E] => Alg[EntityT[F, S, E, *]],
      createRepository: Repository[F, ID, Alg] => RepositoryAlg[F],
      createEffector: StateReader[ReaderT[F, S, *], S] => Effector[ReaderT[F, S, *]],
      emptyState: S,
      customizeBehavior: EventSourcedBehavior[Command, E, S] => EventSourcedBehavior[Command, E, S]
  )(implicit
      sharding: ClusterSharding,
      actorSystem: ActorSystem[_],
      nameProvider: EntityNameProvider[ID],
      idEncoder: EntityIDEncoder[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      askTimeout: Timeout
  ) {
    private implicit val interpretedEntityAlg: Alg[EntityT[F, S, E, *]] = createEntity(
      EntityT.instance
    )
    private implicit val interpretedEffector: ReaderT[F, S, Unit] = createEffector(
      StateReaderT.instance
    ).afterPersist
    private val entityTypeKey = EntityTypeKey[Command](nameProvider())
    private implicit val commandRouter: CommandRouter[F, ID] = ShardingCommandRouter.apply

    def apply: Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] =
      Dispatcher[F].map { implicit dispatcher =>
        val akkaEntity = akka.cluster.sharding.typed.scaladsl.Entity(
          EntityTypeKey[Command](nameProvider())
        ) { context =>
          customizeBehavior(
            EventSourcedBehavior
              .withEnforcedReplies[Command, E, S](
                PersistenceId(entityTypeKey.name, context.entityId),
                emptyState,
                commandHandler = handleCommand,
                eventHandler = handleEvent
              )
              .receiveSignal {
                case (state, RecoveryCompleted) =>
                  dispatcher.unsafeRunSync(
                    Logger[F].info(
                      show"Recovery of ${nameProvider()} entity ${context.entityId} completed"
                    ) >> interpretedEffector.run(state)
                  )
                case (_, RecoveryFailed(failure)) =>
                  dispatcher.unsafeRunSync(
                    Logger[F].warn(
                      show"Recovery of ${nameProvider()} entity ${context.entityId} failed with error ${failure.getMessage}"
                    )
                  )
              }
          )
        }
        (createRepository(RepositoryT.apply[F, S, E, ID, Alg]), sharding.init(akkaEntity))
      }

    private def handleEvent(state: S, event: E)(implicit
        eventApplier: EventApplier[S, E],
        dispatcher: Dispatcher[F]
    ) = eventApplier.apply(state, event) match {
      case Left(error) =>
        dispatcher.unsafeRunSync(Logger[F].warn(error))
        throw new EventApplierException(error)
      case Right(newState) => newState
    }

    private def handleCommand(state: S, command: Command)(implicit dispatcher: Dispatcher[F]) = {
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
              .thenRun((state: S) => dispatcher.unsafeRunSync(interpretedEffector.run(state)))
              .thenReply(command.replyTo) { _: S =>
                Reply(incomingCommand.replyEncoder.encode(reply))
              }
              .pure
        }
      dispatcher.unsafeRunSync(effect)
    }
  }

}
