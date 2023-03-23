package endless.runtime.akka.deploy.internal

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext, EntityTypeKey}
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import endless.core.entity._
import endless.runtime.akka.data._

trait ShardingDeployer[F[_], ID] {
  def nameProvider: EntityNameProvider[ID]
  protected lazy val entityTypeKey: EntityTypeKey[Command] = EntityTypeKey[Command](nameProvider())

  def deployShardedEntity(
      sharding: ClusterSharding,
      customizeEntity: akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[
        Command
      ]] => akka.cluster.sharding.typed.scaladsl.Entity[Command, ShardingEnvelope[Command]]
  )(implicit
      async: Async[F]
  ): Resource[F, ActorRef[ShardingEnvelope[Command]]] =
    Dispatcher.parallel[F].map { implicit dispatcher =>
      val akkaEntity =
        akka.cluster.sharding.typed.scaladsl.Entity(entityTypeKey) { implicit context =>
          Behaviors.setup { implicit actor => createBehavior() }
        }
      sharding.init(customizeEntity(akkaEntity))
    }

  protected def createBehavior()(implicit
      dispatcher: Dispatcher[F],
      actor: ActorContext[Command],
      context: EntityContext[Command]
  ): Behavior[Command]
}
