package endless.runtime.akka.deploy

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext}
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.util.Timeout
import cats.effect.kernel.{Async, Resource}
import endless.core.entity.*
import endless.core.event.EventApplier
import endless.core.interpret.*
import endless.core.protocol.{CommandProtocol, CommandSender, EntityIDCodec}
import endless.runtime.akka.data.*
import AkkaDeployer.*
import endless.runtime.akka.deploy.internal.EventSourcedShardedRepositoryDeployer
import org.typelevel.log4cats.Logger
import endless.core.entity.Deployer
import endless.runtime.akka.ShardingCommandSender

trait AkkaDeployer extends Deployer {
  type DeploymentParameters[F[_], _, S, E] = AkkaDeploymentParameters[F, S, E]
  type Deployment[F[_], RepositoryAlg[_[_]]] = DeployedAkkaRepository[F, RepositoryAlg]

  override def deployRepository[F[_]: Async, ID: EntityIDCodec, S, E, Alg[_[_]], RepositoryAlg[_[
      _
  ]]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      behavior: BehaviorInterpreter[F, S, E, Alg],
      sideEffect: SideEffectInterpreter[F, S, Alg, RepositoryAlg]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[ID, Alg],
      eventApplier: EventApplier[S, E],
      parameters: AkkaDeploymentParameters[F, S, E]
  ): Resource[F, DeployedAkkaRepository[F, RepositoryAlg]] = {
    import parameters.*
    implicit val sharding: ClusterSharding = akkaCluster.sharding
    implicit val sender: CommandSender[F, ID] = ShardingCommandSender[F, ID]
    for {
      interpretedEntityAlg <- Resource.eval(behavior(EntityT.instance))
      deployment <- new EventSourcedShardedRepositoryDeployer(
        interpretedEntityAlg,
        sideEffect,
        parameters.customizeBehavior
      ).deployShardedRepository(repository, parameters.customizeEntity)
    } yield {
      val (repository, shardRegionActor) = deployment
      DeployedAkkaRepository[F, RepositoryAlg](repository, shardRegionActor)
    }
  }

}

object AkkaDeployer {
  final case class DeployedAkkaRepository[F[_], RepositoryAlg[_[_]]](
      repository: RepositoryAlg[F],
      shardRegionActor: ActorRef[ShardingEnvelope[Command]]
  )

  final case class AkkaDeploymentParameters[F[_], S, E](
      customizeBehavior: (
          EntityContext[Command],
          EventSourcedBehavior[Command, E, Option[S]]
      ) => Behavior[Command] =
        (_: EntityContext[Command], behavior: EventSourcedBehavior[Command, E, Option[S]]) =>
          behavior,
      customizeEntity: akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]] => akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[Command]] =
        identity
  )(implicit
      val logger: Logger[F],
      val akkaCluster: AkkaCluster[F],
      val askTimeout: Timeout
  )

}
