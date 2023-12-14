package endless.runtime.akka.deploy.internal

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import akka.persistence.typed.state.{RecoveryCompleted, RecoveryFailed}
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import endless.core.entity.*
import endless.core.interpret.DurableEntityT.{DurableEntityT, State}
import endless.core.interpret.*
import endless.core.protocol.{CommandProtocol, CommandSender, EntityIDCodec}
import endless.runtime.akka.EntityPassivator
import endless.runtime.akka.data.*
import org.typelevel.log4cats.Logger

private[deploy] class DurableShardedRepositoryDeployer[F[
    _
]: Async: Logger, S, ID: EntityIDCodec, Alg[_[
    _
]], RepositoryAlg[_[_]]](
    interpretedEntityAlg: Alg[DurableEntityT[F, S, *]],
    sideEffectInterpreter: SideEffectInterpreter[F, S, Alg, RepositoryAlg],
    customizeBehavior: (
        EntityContext[Command],
        DurableStateBehavior[Command, Option[S]]
    ) => Behavior[Command]
)(implicit
    val nameProvider: EntityNameProvider[ID],
    commandProtocol: CommandProtocol[ID, Alg],
    commandSender: CommandSender[F, ID]
) extends ShardedRepositoryDeployer[F, RepositoryAlg, Alg, ID] {

  protected override def createBehaviorFor(repositoryAlg: RepositoryAlg[F])(implicit
      dispatcher: Dispatcher[F],
      actor: ActorContext[Command],
      context: EntityContext[Command]
  ): Behavior[Command] = {
    implicit val passivator: EntityPassivator[F] = dispatcher.unsafeRunSync(EntityPassivator[F])
    implicit val repository: RepositoryAlg[F] = repositoryAlg
    implicit val entity: Alg[F] =
      Sharding[F, ID, Alg].entityFor(implicitly[EntityIDCodec[ID]].decode(context.entityId))
    implicit val sideEffect: SideEffect[F, S, Alg] =
      dispatcher.unsafeRunSync(sideEffectInterpreter(repository, entity))
    customizeBehavior(
      context,
      DurableStateBehavior
        .withEnforcedReplies[Command, Option[S]](
          PersistenceId(entityTypeKey.name, context.entityId),
          Option.empty[S],
          commandHandler = handleCommand
        )
        .receiveSignal {
          case (state, RecoveryCompleted) =>
            dispatcher.unsafeRunAndForget(
              Logger[F].info(
                show"Recovery of ${nameProvider()} entity ${context.entityId} completed"
              ) >> handleSideEffect(state, SideEffect.Trigger.AfterRecovery)
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

  private def handleCommand(state: Option[S], command: Command)(implicit
      entity: Alg[F],
      dispatcher: Dispatcher[F],
      passivator: EntityPassivator[F],
      sideEffect: SideEffect[F, S, Alg]
  ) = {
    val incomingCommand =
      commandProtocol.server[DurableEntityT[F, S, *]].decode(command.payload)
    val effect = Logger[F].debug(
      show"Handling command for ${nameProvider()} entity ${command.id}"
    ) >> incomingCommand
      .runWith(interpretedEntityAlg)
      .run(state match {
        case Some(value) => DurableEntityT.State.Existing(value)
        case None        => DurableEntityT.State.None
      })
      .flatMap { case (outcome, reply) =>
        (outcome match {
          case State.None           => Effect.none
          case State.Existing(_)    => Effect.none
          case State.Updated(state) => Effect.persist(Option(state))
        })
          .thenRun((state: Option[S]) =>
            // run the effector asynchronously, as it can describe long-running processes
            dispatcher.unsafeRunAndForget(
              handleSideEffect(
                state,
                outcome match {
                  case State.None        => SideEffect.Trigger.AfterRead
                  case State.Existing(_) => SideEffect.Trigger.AfterRead
                  case State.Updated(_)  => SideEffect.Trigger.AfterPersistence
                }
              )
            )
          )
          .thenReply(command.replyTo) { (_: Option[S]) =>
            Reply(incomingCommand.replyEncoder.encode(reply))
          }
          .pure[F]
      }
    dispatcher.unsafeRunSync(effect)
  }
}
