package endless.runtime.pekko.deploy

import cats.effect.kernel.{Async, Resource}
import cats.tagless.FunctorK
import endless.core.entity._
import endless.core.event.EventApplier
import endless.core.interpret._
import endless.core.protocol.{CommandProtocol, EntityIDCodec}
import endless.runtime.pekko.data._
import endless.runtime.pekko.deploy.PekkoDeployer.{
  DeployedPekkoRepository,
  PekkoDeploymentParameters
}
import endless.runtime.pekko.deploy.internal.EventSourcedShardedEntityDeployer
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.cluster.sharding.typed.ShardingEnvelope
import org.apache.pekko.cluster.sharding.typed.scaladsl.EntityContext
import org.apache.pekko.persistence.typed.scaladsl.EventSourcedBehavior
import org.apache.pekko.util.Timeout
import org.typelevel.log4cats.Logger

trait PekkoDeployer extends Deployer {
  type DeploymentParameters[F[_], _, S, E] = PekkoDeploymentParameters[F, S, E]
  type Deployment[F[_], RepositoryAlg[_[_]]] = DeployedPekkoRepository[F, RepositoryAlg]

  def deployRepository[F[_]: Async, ID: EntityIDCodec, S, E, Alg[_[_]]: FunctorK, RepositoryAlg[_[
      _
  ]]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      entity: EntityInterpreter[F, S, E, Alg],
      effector: EffectorInterpreter[F, S, Alg, RepositoryAlg]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[Alg],
      eventApplier: EventApplier[S, E],
      parameters: PekkoDeploymentParameters[F, S, E]
  ): Resource[F, DeployedPekkoRepository[F, RepositoryAlg]] = {
    import parameters._
    for {
      interpretedEntityAlg <- Resource.eval(entity(EntityT.instance))
      deployment <- new EventSourcedShardedEntityDeployer(
        interpretedEntityAlg,
        effector,
        parameters.customizeBehavior
      ).deployShardedRepository(repository, parameters.customizeEntity)
    } yield {
      val (repository, shardRegionActor) = deployment
      DeployedPekkoRepository[F, RepositoryAlg](repository, shardRegionActor)
    }
  }
}

object PekkoDeployer {

  final case class DeployedPekkoRepository[F[_], RepositoryAlg[_[_]]](
      repository: RepositoryAlg[F],
      shardRegionActor: ActorRef[ShardingEnvelope[Command]]
  )

  final case class PekkoDeploymentParameters[F[_], S, E](
      customizeBehavior: (
          EntityContext[Command],
          EventSourcedBehavior[Command, E, Option[S]]
      ) => Behavior[Command] =
        (_: EntityContext[Command], behavior: EventSourcedBehavior[Command, E, Option[S]]) =>
          behavior,
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
