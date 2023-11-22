package endless.runtime.akka

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityContext}
import cats.Applicative
import cats.effect.kernel.{Ref, Sync}
import cats.syntax.eq._
import cats.syntax.flatMap._
import cats.syntax.functor._
import endless.core.entity.Effector.PassivationState

import scala.concurrent.duration.{Duration, FiniteDuration}

private[akka] class EntityPassivator[F[_]: Sync](upcomingPassivation: Ref[F, Option[Cancellable]])(
    implicit
    entityContext: EntityContext[_],
    actorContext: ActorContext[_]
) {
  private lazy val passivateMessage = ClusterSharding.Passivate(actorContext.self)
  private lazy val passivate = Sync[F].delay(entityContext.shard.tell(passivateMessage))
  private lazy val disablePassivation =
    upcomingPassivation.modify(maybeCancellable => {
      (None, maybeCancellable.foreach(_.cancel()))
    })

  def apply(passivationState: PassivationState): F[Unit] = passivationState match {
    case PassivationState.After(duration) => enablePassivation(duration)
    case PassivationState.Disabled        => disablePassivation
    case PassivationState.Unchanged       => Applicative[F].unit
  }

  private def enablePassivation(after: FiniteDuration) =
    if (after === Duration.Zero) passivate else schedulePassivation(after)

  private def schedulePassivation(after: FiniteDuration) =
    disablePassivation >> upcomingPassivation.set(
      Some(
        actorContext.scheduleOnce(after, entityContext.shard, passivateMessage)
      )
    )

}

object EntityPassivator {
  def apply[F[_]: Sync](implicit
      entityContext: EntityContext[_],
      actorContext: ActorContext[_]
  ): F[EntityPassivator[F]] =
    Ref.of[F, Option[Cancellable]](Option.empty[Cancellable]).map(new EntityPassivator(_))
}
