package endless.runtime.pekko.deploy

import cats.effect.kernel.{Async, Resource}
import endless.core.entity._
import endless.core.interpret._
import endless.core.protocol.{CommandProtocol, CommandSender, EntityIDCodec}
import endless.runtime.pekko.ShardingCommandSender
import endless.runtime.pekko.data._
import endless.runtime.pekko.deploy.PekkoDurableDeployer.{
  DeployedPekkoDurableRepository,
  PekkoDurableDeploymentParameters
}
import endless.runtime.pekko.deploy.internal.DurableShardedEntityDeployer
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.cluster.sharding.typed.ShardingEnvelope
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext}
import org.apache.pekko.persistence.typed.state.scaladsl.DurableStateBehavior
import org.apache.pekko.util.Timeout
import org.typelevel.log4cats.Logger

trait PekkoDurableDeployer extends DurableDeployer {
  type DurableDeploymentParameters[F[_], _, S] = PekkoDurableDeploymentParameters[F, S]
  type DurableDeployment[F[_], RepositoryAlg[_[_]]] =
    DeployedPekkoDurableRepository[F, RepositoryAlg]

  override def deployDurableRepository[F[_]: Async, ID: EntityIDCodec, S, Alg[_[_]], RepositoryAlg[
      _[_]
  ]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      behavior: DurableBehaviorInterpreter[F, S, Alg],
      sideEffect: SideEffectInterpreter[F, S, Alg, RepositoryAlg]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[ID, Alg],
      parameters: PekkoDurableDeploymentParameters[F, S]
  ): Resource[F, DeployedPekkoDurableRepository[F, RepositoryAlg]] = {
    import parameters._
    implicit val sharding: ClusterSharding = pekkoCluster.sharding
    implicit val sender: CommandSender[F, ID] = ShardingCommandSender[F, ID]

    for {
      interpretedEntityAlg <- Resource.eval(behavior(DurableEntityT.instance))
      deployment <- new DurableShardedEntityDeployer(
        interpretedEntityAlg,
        sideEffect,
        parameters.customizeBehavior
      ).deployShardedRepository(repository, parameters.customizeEntity)
    } yield {
      val (repository, shardRegionActor) = deployment
      DeployedPekkoDurableRepository[F, RepositoryAlg](repository, shardRegionActor)
    }
  }

}

object PekkoDurableDeployer {
  final case class DeployedPekkoDurableRepository[F[_], RepositoryAlg[_[_]]](
      repository: RepositoryAlg[F],
      shardRegionActor: ActorRef[ShardingEnvelope[Command]]
  )

  final case class PekkoDurableDeploymentParameters[F[_], S](
      customizeBehavior: (
          EntityContext[Command],
          DurableStateBehavior[Command, Option[S]]
      ) => Behavior[Command] =
        (_: EntityContext[Command], behavior: DurableStateBehavior[Command, Option[S]]) => behavior,
      customizeEntity: org.apache.pekko.cluster.sharding.typed.scaladsl.Entity[
        Command,
        ShardingEnvelope[
          Command
        ]
      ] => org.apache.pekko.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]] = identity
  )(implicit
      val logger: Logger[F],
      val pekkoCluster: PekkoCluster[F],
      val askTimeout: Timeout
  )
}
