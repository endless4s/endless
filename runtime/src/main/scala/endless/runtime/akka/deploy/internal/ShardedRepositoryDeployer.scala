package endless.runtime.akka.deploy.internal

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext, EntityTypeKey}
import akka.util.Timeout
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.tagless.FunctorK
import endless.core.entity._
import endless.core.interpret.RepositoryT
import endless.core.protocol.{CommandProtocol, CommandRouter, EntityIDEncoder}
import endless.runtime.akka.ShardingCommandRouter
import endless.runtime.akka.data._
import endless.runtime.akka.deploy.AkkaCluster
import org.typelevel.log4cats.Logger

trait ShardedRepositoryDeployer[F[_], RepositoryAlg[_[_]], Alg[_[_]], ID] {
  implicit def nameProvider: EntityNameProvider[ID]
  protected lazy val entityTypeKey: EntityTypeKey[Command] = EntityTypeKey[Command](nameProvider())

  def deployShardedRepository(
      createRepository: Repository[F, ID, Alg] => F[RepositoryAlg[F]],
      customizeEntity: akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]] => akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[Command]]
  )(implicit
      akkaCluster: AkkaCluster,
      entityIDEncoder: EntityIDEncoder[ID],
      async: Async[F],
      logger: Logger[F],
      askTimeout: Timeout,
      commandProtocol: CommandProtocol[Alg],
      functorK: FunctorK[Alg]
  ): Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] = {
    implicit val clusterSharding: ClusterSharding = akkaCluster.sharding
    implicit val commandRouter: CommandRouter[F, ID] = ShardingCommandRouter.apply
    val repositoryT = RepositoryT.apply[F, ID, Alg]
    Resource
      .eval(createRepository(repositoryT))
      .flatMap(repository =>
        Dispatcher.parallel[F].map { implicit dispatcher =>
          val akkaEntity =
            akka.cluster.sharding.typed.scaladsl.Entity(entityTypeKey) { implicit context =>
              Behaviors.setup { implicit actor => createBehaviorFor(repository, repositoryT) }
            }
          (repository, akkaCluster.sharding.init(customizeEntity(akkaEntity)))
        }
      )
  }

  protected def createBehaviorFor(
      repository: RepositoryAlg[F],
      repositoryT: RepositoryT[F, ID, Alg]
  )(implicit
      dispatcher: Dispatcher[F],
      actor: ActorContext[Command],
      context: EntityContext[Command]
  ): Behavior[Command]
}
