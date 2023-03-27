package endless.runtime.akka.deploy

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cats.effect.kernel.{Async, Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import org.typelevel.log4cats.Logger

/** Actor system and cluster sharding extension.
  * @param system
  *   actor system
  * @param sharding
  *   cluster sharding extension
  */
final case class AkkaCluster(system: ActorSystem[_], sharding: ClusterSharding)

object AkkaCluster {

  /** Create a resource that manages the lifetime of an Akka actor system with cluster sharding
    * extension. The actor system is created when the resource is acquired and shutdown when the
    * resource is released. Akka's built-in SIGTERM hook is disabled in the reference config, so
    * that CoordinatedShutdown is only triggered here, in the release action.
    */
  def managedResource[F[_]: Async: Logger](
      createActorSystem: => ActorSystem[_]
  ): Resource[F, AkkaCluster] = Resource.make(initCluster(createActorSystem))(shutdownCluster[F])

  private def initCluster[F[_]: Sync: Logger](
      createActorSystem: => ActorSystem[_]
  ): F[AkkaCluster] =
    Logger[F].debug("Joining Akka actor cluster") >> Sync[F]
      .delay {
        val actorSystem = createActorSystem
        val sharding = ClusterSharding(actorSystem)
        AkkaCluster(actorSystem, sharding)
      }
      .flatTap(cluster =>
        Logger[F].info(show"Joined Akka actor cluster with system name ${cluster.system.name}")
      )

  private def shutdownCluster[F[_]: Async: Logger](cluster: AkkaCluster) =
    Logger[F].info("Leaving Akka actor cluster") >> Async[F]
      .fromFuture(
        Async[F].delay(
          CoordinatedShutdown(cluster.system).run(CoordinatedShutdown.ActorSystemTerminateReason)
        )
      )
      .void >> Logger[F].info("Akka cluster exited and actor system shutdown complete")
}
