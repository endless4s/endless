package endless.runtime.pekko.deploy

import cats.effect.kernel.{Async, Resource}
import cats.tagless.FunctorK
import endless.core.entity._
import endless.core.interpret._
import endless.core.protocol.{CommandProtocol, EntityIDCodec}
import endless.runtime.pekko.data._
import endless.runtime.pekko.deploy.PekkoDurableDeployer.{
  DeployedPekkoDurableRepository,
  PekkoDurableDeploymentParameters
}
import endless.runtime.pekko.deploy.internal.DurableShardedEntityDeployer
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.cluster.sharding.typed.ShardingEnvelope
import org.apache.pekko.cluster.sharding.typed.scaladsl.EntityContext
import org.apache.pekko.persistence.typed.state.scaladsl.DurableStateBehavior
import org.apache.pekko.util.Timeout
import org.typelevel.log4cats.Logger

trait PekkoDurableDeployer extends DurableDeployer {
  type DurableDeploymentParameters[F[_], _, S] = PekkoDurableDeploymentParameters[F, S]
  type DurableDeployment[F[_], RepositoryAlg[_[_]]] =
    DeployedPekkoDurableRepository[F, RepositoryAlg]

  def deployDurableRepository[F[_]: Async, ID: EntityIDCodec, S, Alg[_[_]]: FunctorK, RepositoryAlg[
      _[_]
  ]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      entity: DurableEntityInterpreter[F, S, Alg],
      effector: EffectorInterpreter[F, S, Alg, RepositoryAlg]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[Alg],
      parameters: PekkoDurableDeploymentParameters[F, S]
  ): Resource[F, DeployedPekkoDurableRepository[F, RepositoryAlg]] = {
    import parameters._
    for {
      interpretedEntityAlg <- Resource.eval(entity(DurableEntityT.instance))
      deployment <- new DurableShardedEntityDeployer(
        interpretedEntityAlg,
        effector,
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
      val PekkoCluster: PekkoCluster[F],
      val askTimeout: Timeout
  )
}
