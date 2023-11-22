package endless.runtime.pekko.deploy.internal

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.cluster.sharding.typed.scaladsl.EntityContext
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import org.apache.pekko.persistence.typed.{PersistenceId, RecoveryCompleted, RecoveryFailed}
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import endless.core.entity.{Effector, EntityNameProvider, Repository}
import endless.core.event.EventApplier
import endless.core.interpret.{EffectorInterpreter, EntityT}
import endless.core.protocol.{CommandProtocol, CommandSender, EntityIDCodec}
import endless.runtime.pekko.EntityPassivator
import endless.runtime.pekko.data.{Command, Reply}
import endless.runtime.pekko.deploy.internal.EventSourcedShardedEntityDeployer._
import org.typelevel.log4cats.Logger

private[deploy] class EventSourcedShardedEntityDeployer[F[
    _
]: Async: Logger, S, E, ID: EntityIDCodec, Alg[_[_]], RepositoryAlg[_[_]]](
    interpretedEntityAlg: Alg[EntityT[F, S, E, *]],
    createEffectorInterpreter: F[EffectorInterpreter[F, S, Alg, RepositoryAlg]],
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
    implicit val repository: RepositoryAlg[F] = repositoryAlg
    implicit val entity: Alg[F] =
      Repository[F, ID, Alg].entityFor(implicitly[EntityIDCodec[ID]].decode(context.entityId))
    implicit val effectorInterpreter: EffectorInterpreter[F, S, Alg, RepositoryAlg] =
      dispatcher.unsafeRunSync(createEffectorInterpreter)
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
            dispatcher.unsafeRunAndForget(
              Logger[F].info(
                show"Recovery of ${nameProvider()} entity ${context.entityId} completed"
              ) >> handleSideEffects(effectorInterpreter, repositoryAlg, entity, passivator, state)
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

  private def handleSideEffects(
      effectorInterpreter: EffectorInterpreter[F, S, Alg, RepositoryAlg],
      repositoryAlg: RepositoryAlg[F],
      entity: Alg[F],
      passivator: EntityPassivator[F],
      state: Option[S]
  ) = {
    for {
      effector <- Effector[F, S, Alg](entity, state)
      _ <- effectorInterpreter.apply(effector, repositoryAlg, entity)
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
      repository: RepositoryAlg[F],
      dispatcher: Dispatcher[F],
      passivator: EntityPassivator[F],
      effectorInterpreter: EffectorInterpreter[F, S, Alg, RepositoryAlg]
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
            .thenRun((state: Option[S]) =>
              // run the effector asynchronously, as it can describe long-running processes
              dispatcher
                .unsafeRunAndForget(
                  handleSideEffects(effectorInterpreter, repository, entity, passivator, state)
                )
            )
            .thenReply(command.replyTo) { _: Option[S] =>
              Reply(incomingCommand.replyEncoder.encode(reply))
            }
            .pure[F]
        case Right((_, reply)) =>
          Effect
            .reply[Reply, E, Option[S]](command.replyTo)(
              Reply(incomingCommand.replyEncoder.encode(reply))
            )
            .pure[F]
      }
    dispatcher.unsafeRunSync(effect)
  }
}

private[deploy] object EventSourcedShardedEntityDeployer {
  final class EventApplierException(error: String) extends RuntimeException(error)
}
