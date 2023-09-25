package endless.runtime.akka.deploy

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.util.Timeout
import cats.effect.kernel.{Async, Resource}
import cats.tagless.FunctorK
import endless.core.entity._
import endless.core.event.EventApplier
import endless.core.interpret._
import endless.core.protocol.{CommandProtocol, EntityIDCodec}
import endless.runtime.akka.data._
import AkkaDeployer._
import endless.runtime.akka.deploy.internal.EventSourcedShardedEntityDeployer
import org.typelevel.log4cats.Logger
import endless.core.entity.Deployer

trait AkkaDeployer extends Deployer {
  type DeploymentParameters[F[_], _, S, E] = AkkaDeploymentParameters[F, S, E]
  type Deployment[F[_], RepositoryAlg[_[_]]] = DeployedAkkaRepository[F, RepositoryAlg]

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
      parameters: AkkaDeploymentParameters[F, S, E]
  ): Resource[F, DeployedAkkaRepository[F, RepositoryAlg]] = {
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
