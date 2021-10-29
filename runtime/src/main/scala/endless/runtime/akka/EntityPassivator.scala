package endless.runtime.akka

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext}
import cats.syntax.eq._
import endless.core.interpret.EffectorT.PassivationState

import scala.concurrent.duration.{Duration, FiniteDuration}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
private[akka] class EntityPassivator(
    entityContext: EntityContext[_],
    actorContext: ActorContext[_]
) {
  // this mutable state is only ever accessed from the actor thread => no concurrency risk
  private var upcomingPassivation: Option[Cancellable] = Option.empty[Cancellable]

  def apply(passivationState: PassivationState): Unit = passivationState match {
    case PassivationState.After(duration) => enablePassivation(duration)
    case PassivationState.Disabled        => disablePassivation()
  }

  private def enablePassivation(after: FiniteDuration = Duration.Zero): Unit = {
    if (after === Duration.Zero) passivate() else schedulePassivation(after)
  }

  private def disablePassivation(): Unit = {
    upcomingPassivation.foreach(_.cancel())
    upcomingPassivation = None
  }

  private def passivate(): Unit =
    entityContext.shard.tell(ClusterSharding.Passivate(actorContext.self))

  private def schedulePassivation(after: FiniteDuration): Unit = {
    upcomingPassivation.foreach(_.cancel())
    upcomingPassivation = Some(
      actorContext.scheduleOnce(
        after,
        entityContext.shard,
        ClusterSharding.Passivate(actorContext.self)
      )
    )
  }

}
