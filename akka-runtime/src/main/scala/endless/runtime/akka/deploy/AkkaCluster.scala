package endless.runtime.akka.deploy

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.cluster.{Cluster, MemberStatus}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cats.effect.kernel.implicits.*
import cats.effect.kernel.{Async, Deferred, Resource, Sync}
import cats.effect.std.Dispatcher
import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import org.typelevel.log4cats.Logger

import scala.concurrent.TimeoutException
import scala.concurrent.duration.{Duration, DurationInt}

/** Actor system and cluster sharding extension as well as dispatcher tied to its resource scope.
  * @param system
  *   actor system
  * @param cluster
  *   cluster extension
  * @param sharding
  *   cluster sharding extension
  * @param dispatcher
  *   effects dispatcher tied to the cluster resource scope
  */
final case class AkkaCluster[F[_]: Async](
    system: ActorSystem[?],
    dispatcher: Dispatcher[F],
    cluster: Cluster,
    sharding: ClusterSharding
) {

  /** Returns true if the cluster member is up. Can be used for readiness checks.
    */
  def isMemberUp: F[Boolean] = Sync[F].delay(cluster.selfMember.status match {
    case MemberStatus.Up => true
    case _               => false
  })
}

object AkkaCluster {

  /** Create a resource that manages the lifetime of an Akka actor system with cluster sharding
    * extension. The actor system is created when the resource is acquired and shutdown when the
    * resource is released.
    *
    * @see
    *   Some elements borrowed from
    *   https://alexn.org/blog/2023/04/17/integrating-akka-with-cats-effect-3/
    *
    * @param createActorSystem
    *   Actor system creator. It is recommended to use the IO execution context
    *   (`IO.executionContext`) for the actor system, as it supports Akka operation and it's simpler
    *   to have a single application execution context
    *
    * @param catsEffectReleaseTimeout
    *   Maximum amount of time Akka coordinated shutdown is allowed to wait for cats-effect to
    *   finish, typically when Akka initiates shutdown following a SBR decision. This value should
    *   not be higher than the actual timeout for `before-service-unbind` phase of Akka coordinated
    *   shutdown. See <a href="https://doc.akka.io/docs/akka/current/coordinated-shutdown.html">Akka
    *   coordinated shutdown documentation</a> to learn how to configure the timeouts of individual
    *   phases. Default (5 seconds) is the same as the default-phase-timeout of Akka coordinated
    *   shutdown.
    * @param akkaReleaseTimeout
    *   Maximum amount of time to wait for the actor system to terminate during resource release (5
    *   seconds by default).
    */
  def managedResource[F[_]: Async: Logger](
      createActorSystem: => ActorSystem[?],
      catsEffectReleaseTimeout: Duration = 5.seconds,
      akkaReleaseTimeout: Duration = 5.seconds
  ): Resource[F, AkkaCluster[F]] =
    Dispatcher
      .parallel(await = true)
      .flatMap(dispatcher =>
        Resource[F, AkkaCluster[F]](for {
          cluster <- createCluster(createActorSystem, dispatcher)
          awaitCatsTermination <- Deferred[F, Unit]
          _ <- Sync[F].delay {
            CoordinatedShutdown(cluster.system)
              .addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "ce-resources-release") { () =>
                dispatcher.unsafeToFuture(
                  awaitCatsTermination.get
                    .timeout(catsEffectReleaseTimeout)
                    .recoverWith { case ex: TimeoutException =>
                      Logger[F].error(ex)(
                        "Timed out during cluster shutdown while waiting for cats-effect resources release"
                      )
                    }
                    .as(Done)
                )
              }
          }
        } yield {
          val release = for {
            _ <- awaitCatsTermination.complete(())
            _ <- Logger[F].info("Leaving Akka actor cluster")
            _ <- Async[F]
              .fromFuture(
                Async[F].blocking(
                  CoordinatedShutdown(cluster.system)
                    .run(CoordinatedShutdown.ActorSystemTerminateReason)
                )
              )
              .void
              .timeoutAndForget(akkaReleaseTimeout)
              .handleErrorWith(
                Logger[F].error(_)(
                  "Timed out during cluster shutdown while waiting for actor system to terminate"
                )
              )
            _ <- Logger[F].info("Akka cluster exited and actor system shutdown complete")
          } yield ()
          (cluster, release)
        })
      )

  private def createCluster[F[_]: Async: Logger](
      createActorSystem: => ActorSystem[?],
      dispatcher: Dispatcher[F]
  ) = for {
    system <- Sync[F].delay(createActorSystem)
    _ <- Logger[F].info(s"Created actor system ${system.name}")
    cluster <- Sync[F].delay(Cluster(system))
    _ <- Logger[F].info(s"Created cluster extension for actor system ${system.name}")
    sharding <- Sync[F].delay(ClusterSharding(system))
    _ <- Logger[F].info(s"Created cluster sharding extension for actor system ${system.name}")
  } yield AkkaCluster(system, dispatcher, cluster, sharding)
}
