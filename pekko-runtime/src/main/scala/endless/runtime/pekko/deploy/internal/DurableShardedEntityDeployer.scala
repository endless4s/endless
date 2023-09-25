package endless.runtime.pekko.deploy.internal

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.cluster.sharding.typed.scaladsl.EntityContext
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import org.apache.pekko.persistence.typed.state.{RecoveryCompleted, RecoveryFailed}
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.show._
import cats.tagless.FunctorK
import endless.core.entity._
import endless.core.interpret.DurableEntityT.{DurableEntityT, State}
import endless.core.interpret.EffectorT._
import endless.core.interpret._
import endless.core.protocol.{CommandProtocol, EntityIDCodec}
import endless.runtime.pekko.EntityPassivator
import endless.runtime.pekko.data._
import org.typelevel.log4cats.Logger

private[deploy] class DurableShardedEntityDeployer[F[_]: Async: Logger, S, ID: EntityIDCodec, Alg[
    _[_]
]: FunctorK, RepositoryAlg[_[_]]](
    interpretedEntityAlg: Alg[DurableEntityT[F, S, *]],
    createEffector: EffectorInterpreter[F, S, Alg, RepositoryAlg],
    customizeBehavior: (
        EntityContext[Command],
        DurableStateBehavior[Command, Option[S]]
    ) => Behavior[Command]
)(implicit val nameProvider: EntityNameProvider[ID], commandProtocol: CommandProtocol[Alg])
    extends ShardedRepositoryDeployer[F, RepositoryAlg, Alg, ID] {

  protected override def createBehaviorFor(
      repository: RepositoryAlg[F],
      repositoryT: RepositoryT[F, ID, Alg]
  )(implicit
      dispatcher: Dispatcher[F],
      actor: ActorContext[Command],
      context: EntityContext[Command]
  ): Behavior[Command] = {
    implicit val passivator: EntityPassivator[F] =
      dispatcher.unsafeRunSync(EntityPassivator[F])
    implicit val entity: Alg[F] =
      repositoryT.entityFor(implicitly[EntityIDCodec[ID]].decode(context.entityId))
    implicit val effector: EffectorT[F, S, Alg, Unit] =
      dispatcher.unsafeRunSync(createEffector(EffectorT.instance, repository, entity))
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
              ) >> effector.runS(state, entity).flatMap(passivator.apply)
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

  private def handleCommand(state: Option[S], command: Command)(implicit
      entity: Alg[F],
      dispatcher: Dispatcher[F],
      passivator: EntityPassivator[F],
      effector: EffectorT[F, S, Alg, Unit]
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
      .flatMap { case (state, reply) =>
        (state match {
          case State.None           => Effect.none
          case State.Existing(_)    => Effect.none
          case State.Updated(state) => Effect.persist(Option(state))
        })
          .thenRun((state: Option[S]) =>
            // run the effector asynchronously, as it can describe long-running processes
            dispatcher.unsafeRunAndForget(effector.runS(state, entity).flatMap(passivator.apply))
          )
          .thenReply(command.replyTo) { _: Option[S] =>
            Reply(incomingCommand.replyEncoder.encode(reply))
          }
          .pure[F]
      }
    dispatcher.unsafeRunSync(effect)
  }
}
