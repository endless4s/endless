package endless.runtime.akka.deploy

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.state.scaladsl.DurableStateBehavior
import akka.util.Timeout
import cats.effect.kernel.{Async, Resource}
import cats.tagless.FunctorK
import endless.core.entity._
import endless.core.interpret._
import endless.core.protocol.{CommandProtocol, EntityIDCodec}
import endless.runtime.akka.data._
import endless.runtime.akka.deploy.AkkaDurableDeployer._
import endless.runtime.akka.deploy.internal.DurableShardedEntityDeployer
import org.typelevel.log4cats.Logger

trait AkkaDurableDeployer extends DurableDeployer {
  type DurableDeploymentParameters[F[_], _, S] = AkkaDurableDeploymentParameters[F, S]
  type DurableDeployment[F[_], RepositoryAlg[_[_]]] =
    DeployedAkkaDurableRepository[F, RepositoryAlg]

  def deployDurableRepository[F[_]: Async, ID: EntityIDCodec, S, Alg[_[_]]: FunctorK, RepositoryAlg[
      _[_]
  ]](
      repository: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      entity: DurableEntityInterpreter[F, S, Alg],
      effector: EffectorInterpreter[F, S, Alg, RepositoryAlg]
  )(implicit
      nameProvider: EntityNameProvider[ID],
      commandProtocol: CommandProtocol[Alg],
      parameters: AkkaDurableDeploymentParameters[F, S]
  ): Resource[F, DeployedAkkaDurableRepository[F, RepositoryAlg]] = {
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
