package akka.persistence.tagless.runtime

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.tagless.core.interpret.EntityT
import akka.persistence.tagless.core.typeclass.{CommandProtocol, Encoded, Partitioner}
import akka.util.Timeout
import cats.data.ReaderT
import cats.effect.kernel.Async
import cats.tagless.FunctorK
import cats.tagless.implicits._
import cats.~>

class ShardPartitioner[F[_], S, E, ID, Alg[_[_]]: FunctorK, Code](
    protocol: CommandProtocol[Alg, Code],
    sharding: ClusterSharding,
    actorSystem: ActorSystem[_],
    askTimeout: Timeout,
    F: Async[F]
) extends Partitioner[F, S, E, ID] {
  def apply[A](fa: EntityT[F, S, E, A]): ReaderT[EntityT[F, S, E, *], ID, A] =
    protocol.client.mapK(new (Encoded[Code, *] ~> ReaderT[EntityT[F, S, E, *], ID, *]) {
      def apply[A](fa: Encoded[Code, A]): ReaderT[EntityT[F, S, E, *], ID, A] = ???
    })
}
