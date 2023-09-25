package endless.runtime.pekko.deploy.internal

import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.cluster.sharding.typed.ShardingEnvelope
import org.apache.pekko.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  EntityContext,
  EntityTypeKey
}
import org.apache.pekko.util.Timeout
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.tagless.FunctorK
import endless.core.entity._
import endless.core.interpret.{RepositoryInterpreter, RepositoryT}
import endless.core.protocol.{CommandProtocol, CommandRouter, EntityIDEncoder}
import endless.runtime.pekko.ShardingCommandRouter
import endless.runtime.pekko.data._
import endless.runtime.pekko.deploy.PekkoCluster
import org.typelevel.log4cats.Logger

trait ShardedRepositoryDeployer[F[_], RepositoryAlg[_[_]], Alg[_[_]], ID] {
  implicit def nameProvider: EntityNameProvider[ID]
  protected lazy val entityTypeKey: EntityTypeKey[Command] = EntityTypeKey[Command](nameProvider())

  def deployShardedRepository(
      repositoryInterpreter: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
      customizeEntity: org.apache.pekko.cluster.sharding.typed.scaladsl.Entity[
        Command,
        ShardingEnvelope[
          Command
        ]
      ] => org.apache.pekko.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]]
  )(implicit
      pekkoCluster: PekkoCluster[F],
      entityIDEncoder: EntityIDEncoder[ID],
      async: Async[F],
      logger: Logger[F],
      askTimeout: Timeout,
      commandProtocol: CommandProtocol[Alg],
      functorK: FunctorK[Alg]
  ): Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] = {
    implicit val clusterSharding: ClusterSharding = pekkoCluster.sharding
    implicit val commandRouter: CommandRouter[F, ID] = ShardingCommandRouter.apply
    val repositoryT = RepositoryT.apply[F, ID, Alg]
    Resource
      .eval(repositoryInterpreter(repositoryT))
      .map(repository => {
        implicit val dispatcher: Dispatcher[F] = pekkoCluster.dispatcher
        val pekkoEntity =
          org.apache.pekko.cluster.sharding.typed.scaladsl.Entity(entityTypeKey) {
            implicit context =>
              Behaviors.setup { implicit actor => createBehaviorFor(repository, repositoryT) }
          }
        (repository, pekkoCluster.sharding.init(customizeEntity(pekkoEntity)))
      })
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
