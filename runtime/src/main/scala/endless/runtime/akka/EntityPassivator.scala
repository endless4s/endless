package endless.runtime.akka

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext}
import cats.Applicative
import cats.effect.kernel.{Ref, Sync}
import cats.syntax.eq._
import cats.syntax.flatMap._
import endless.core.interpret.EffectorT.PassivationState

import scala.concurrent.duration.{Duration, FiniteDuration}

private[akka] class EntityPassivator[F[_]: Sync](
    entityContext: EntityContext[_],
    actorContext: ActorContext[_]
) {
  private lazy val upcomingPassivation: F[Ref[F, Option[Cancellable]]] =
    Ref.of(Option.empty[Cancellable])
  private lazy val passivateMessage = ClusterSharding.Passivate(actorContext.self)

  def apply(passivationState: PassivationState): F[Unit] = passivationState match {
    case PassivationState.After(duration) => enablePassivation(duration)
    case PassivationState.Disabled        => disablePassivation()
    case PassivationState.Unchanged       => Applicative[F].unit
  }

  private def disablePassivation() =
    upcomingPassivation >>= (_.modify(maybeCancellable => {
      (None, maybeCancellable.foreach(_.cancel()))
    }))

  private def enablePassivation(after: FiniteDuration = Duration.Zero) =
    if (after === Duration.Zero) passivate() else schedulePassivation(after)

  private def passivate() =
    Sync[F].delay(entityContext.shard.tell(passivateMessage))

  private def schedulePassivation(after: FiniteDuration) =
    disablePassivation() >> upcomingPassivation >>= (_.set(
      Some(
        actorContext.scheduleOnce(after, entityContext.shard, passivateMessage)
      )
    ))

}
