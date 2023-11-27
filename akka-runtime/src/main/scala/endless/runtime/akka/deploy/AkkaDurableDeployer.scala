package endless.runtime.akka.deploy

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext}
import akka.persistence.typed.state.scaladsl.DurableStateBehavior
import akka.util.Timeout
import cats.effect.kernel.{Async, Resource}
import endless.core.entity.*
import endless.core.interpret.*
import endless.core.protocol.{CommandProtocol, CommandSender, EntityIDCodec}
import endless.runtime.akka.ShardingCommandSender
import endless.runtime.akka.data.*
import endless.runtime.akka.deploy.AkkaDurableDeployer.{
  AkkaDurableDeploymentParameters,
  DeployedAkkaDurableRepository
}
import endless.runtime.akka.deploy.internal.DurableShardedRepositoryDeployer
import org.typelevel.log4cats.Logger

trait AkkaDurableDeployer extends DurableDeployer {
  type DurableDeploymentParameters[F[_], _, S] = AkkaDurableDeploymentParameters[F, S]
  type DurableDeployment[F[_], RepositoryAlg[_[_]]] =
    DeployedAkkaDurableRepository[F, RepositoryAlg]

  override def deployDurableRepository[F[_]: Async, ID: EntityIDCodec, S, Alg[_[_]], RepositoryAlg[
      _[_]
  ]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      behavior: DurableBehaviorInterpreter[F, S, Alg],
      sideEffect: SideEffectInterpreter[F, S, Alg, RepositoryAlg]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[ID, Alg],
      parameters: AkkaDurableDeploymentParameters[F, S]
  ): Resource[F, DeployedAkkaDurableRepository[F, RepositoryAlg]] = {
    import parameters.*
    implicit val sharding: ClusterSharding = akkaCluster.sharding
    implicit val sender: CommandSender[F, ID] = ShardingCommandSender[F, ID]
    for {
      interpretedEntityAlg <- Resource.eval(behavior(DurableEntityT.instance))
      deployment <- new DurableShardedRepositoryDeployer(
        interpretedEntityAlg,
        sideEffect,
        parameters.customizeBehavior
      ).deployShardedRepository(repository, parameters.customizeEntity)
    } yield {
      val (repository, shardRegionActor) = deployment
      DeployedAkkaDurableRepository[F, RepositoryAlg](repository, shardRegionActor)
    }
  }

}

object AkkaDurableDeployer {
  final case class DeployedAkkaDurableRepository[F[_], RepositoryAlg[_[_]]](
      repository: RepositoryAlg[F],
      shardRegionActor: ActorRef[ShardingEnvelope[Command]]
  )

  final case class AkkaDurableDeploymentParameters[F[_], S](
      customizeBehavior: (
          EntityContext[Command],
          DurableStateBehavior[Command, Option[S]]
      ) => Behavior[Command] =
        (_: EntityContext[Command], behavior: DurableStateBehavior[Command, Option[S]]) => behavior,
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
