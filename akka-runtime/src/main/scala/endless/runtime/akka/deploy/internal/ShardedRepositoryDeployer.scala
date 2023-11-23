package endless.runtime.akka.deploy.internal

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext, EntityTypeKey}
import akka.util.Timeout
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import endless.core.entity._
import endless.core.interpret.RepositoryInterpreter
import endless.core.protocol.{CommandProtocol, CommandSender, EntityIDEncoder}
import endless.runtime.akka.ShardingCommandSender
import endless.runtime.akka.data._
import endless.runtime.akka.deploy.AkkaCluster
import org.typelevel.log4cats.Logger

trait ShardedRepositoryDeployer[F[_], RepositoryAlg[_[_]], Alg[_[_]], ID] {
  implicit def nameProvider: EntityNameProvider[ID]
  protected lazy val entityTypeKey: EntityTypeKey[Command] = EntityTypeKey[Command](nameProvider())

  def deployShardedRepository(
                               repositoryInterpreter: RepositoryInterpreter[F, ID, Alg, RepositoryAlg],
                               customizeEntity: akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]] => akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[Command]]
  )(implicit
      akkaCluster: AkkaCluster[F],
      entityIDEncoder: EntityIDEncoder[ID],
      async: Async[F],
      logger: Logger[F],
      askTimeout: Timeout,
      commandProtocol: CommandProtocol[ID, Alg]
  ): Resource[F, (RepositoryAlg[F], ActorRef[ShardingEnvelope[Command]])] = {
    implicit val clusterSharding: ClusterSharding = akkaCluster.sharding
    implicit val commandSender: CommandSender[F, ID] = ShardingCommandSender.apply
    val repository = Sharding.apply[F, ID, Alg]
    Resource
      .eval(repositoryInterpreter(repository))
      .map(repository => {
        implicit val dispatcher: Dispatcher[F] = akkaCluster.dispatcher
        val akkaEntity =
          akka.cluster.sharding.typed.scaladsl.Entity(entityTypeKey) { implicit context =>
            Behaviors.setup { implicit actor => createBehaviorFor(repository) }
          }
        (repository, akkaCluster.sharding.init(customizeEntity(akkaEntity)))
      })
  }

  protected def createBehaviorFor(repository: RepositoryAlg[F])(implicit
      dispatcher: Dispatcher[F],
      actor: ActorContext[Command],
      context: EntityContext[Command]
  ): Behavior[Command]
}
