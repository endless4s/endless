package endless.runtime.akka.deploy.internal

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted, RecoveryFailed}
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import endless.core.entity.{Effector, EntityNameProvider, Sharding, SideEffect}
import endless.core.event.EventApplier
import endless.core.interpret.{EntityT, SideEffectInterpreter}
import endless.core.protocol.{CommandProtocol, CommandSender, EntityIDCodec}
import endless.runtime.akka.EntityPassivator
import endless.runtime.akka.data.{Command, Reply}
import endless.runtime.akka.deploy.internal.EventSourcedShardedRepositoryDeployer.*
import org.typelevel.log4cats.Logger

private[deploy] class EventSourcedShardedRepositoryDeployer[F[
    _
]: Async: Logger, S, E, ID: EntityIDCodec, Alg[_[_]], RepositoryAlg[_[_]]](
    interpretedEntityAlg: Alg[EntityT[F, S, E, *]],
    sideEffectInterpreter: SideEffectInterpreter[F, S, Alg, RepositoryAlg],
    customizeBehavior: (
        EntityContext[Command],
        EventSourcedBehavior[Command, E, Option[S]]
    ) => Behavior[Command]
)(implicit
    val nameProvider: EntityNameProvider[ID],
    commandProtocol: CommandProtocol[ID, Alg],
    commandSender: CommandSender[F, ID],
    eventApplier: EventApplier[S, E]
) extends ShardedRepositoryDeployer[F, RepositoryAlg, Alg, ID] {

  protected override def createBehaviorFor(repositoryAlg: RepositoryAlg[F])(implicit
      dispatcher: Dispatcher[F],
      actor: ActorContext[Command],
      context: EntityContext[Command]
  ): Behavior[Command] = {
    implicit val passivator: EntityPassivator[F] = dispatcher.unsafeRunSync(EntityPassivator[F])
    implicit val entity: Alg[F] =
      Sharding[F, ID, Alg].entityFor(implicitly[EntityIDCodec[ID]].decode(context.entityId))
    implicit val repository: RepositoryAlg[F] = repositoryAlg
    implicit val sideEffect: SideEffect[F, S, Alg] =
      dispatcher.unsafeRunSync(sideEffectInterpreter(repository, entity))
    customizeBehavior(
      context,
      EventSourcedBehavior
        .withEnforcedReplies[Command, E, Option[S]](
          PersistenceId(entityTypeKey.name, context.entityId),
          Option.empty[S],
          commandHandler = handleCommand,
          eventHandler = handleEvent
        )
        .receiveSignal {
          case (state, RecoveryCompleted) =>
            dispatcher.unsafeRunAndForget {
              Logger[F]
                .info(
                  show"Recovery of ${nameProvider()} entity ${context.entityId} completed"
                ) >> handleSideEffect(state, SideEffect.Trigger.AfterRecovery)
            }
          case (_, RecoveryFailed(failure)) =>
            dispatcher.unsafeRunSync(
              Logger[F].warn(
                show"Recovery of ${nameProvider()} entity ${context.entityId} failed with error ${failure.getMessage}"
              )
            )
        }
    )
  }

  private def handleSideEffect(
      state: Option[S],
      trigger: SideEffect.Trigger
  )(implicit sideEffect: SideEffect[F, S, Alg], entity: Alg[F], passivator: EntityPassivator[F]) = {
    for {
      effector <- Effector[F, S, Alg](entity, state)
      _ <- sideEffect.apply(trigger, effector)
      passivationState <- effector.passivationState
      _ <- passivator.apply(passivationState)
    } yield ()
  }

  private def handleEvent(state: Option[S], event: E)(implicit dispatcher: Dispatcher[F]) =
    eventApplier.apply(state, event) match {
      case Left(error) =>
        dispatcher.unsafeRunSync(Logger[F].warn(error))
        throw new EventApplierException(error)
      case Right(newState) => newState
    }

  private def handleCommand(state: Option[S], command: Command)(implicit
      entity: Alg[F],
      dispatcher: Dispatcher[F],
      passivator: EntityPassivator[F],
      sideEffect: SideEffect[F, S, Alg]
  ) = {
    val incomingCommand =
      commandProtocol.server[EntityT[F, S, E, *]].decode(command.payload)
    val effect = Logger[F].debug(
      show"Handling command for ${nameProvider()} entity ${command.id}"
    ) >> incomingCommand
      .runWith(interpretedEntityAlg)
      .run(state)
      .flatMap {
        case Left(error) =>
          Logger[F].warn(error) >> Effect.unhandled[E, Option[S]].thenNoReply().pure[F]
        case Right((events, reply)) if events.nonEmpty =>
          Effect
            .persist(events.toList)
            .thenRun(
              (state: Option[
                S
              ]) => // run the effector asynchronously, as it can describe long-running processes
                dispatcher
                  .unsafeRunAndForget(handleSideEffect(state, SideEffect.Trigger.AfterPersistence))
            )
            .thenReply(command.replyTo) { (_: Option[S]) =>
              Reply(incomingCommand.replyEncoder.encode(reply))
            }
            .pure[F]
        case Right((_, reply)) =>
          Effect
            .none[E, Option[S]]
            .thenRun((state: Option[S]) =>
              dispatcher
                .unsafeRunAndForget(handleSideEffect(state, SideEffect.Trigger.AfterRead))
            )
            .thenReply[Reply](command.replyTo) { (_: Option[S]) =>
              Reply(incomingCommand.replyEncoder.encode(reply))
            }
            .pure[F]
      }
    dispatcher.unsafeRunSync(effect)
  }
}

private[deploy] object EventSourcedShardedRepositoryDeployer {
  final class EventApplierException(error: String) extends RuntimeException(error)
}
