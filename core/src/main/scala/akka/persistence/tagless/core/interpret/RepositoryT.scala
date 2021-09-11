package akka.persistence.tagless.core.interpret
import akka.persistence.tagless.core.data.{EventsFolder, Folded}
import akka.persistence.tagless.core.typeclass.{Command, EventApplier, Partitioner, Repository}
import cats.data.ReaderT
import cats.tagless.FunctorK
import cats.tagless.implicits._

final class RepositoryT[F[_], S, E, ID, Alg[_[_]]: FunctorK](implicit
    interpretedAlgebra: Alg[EntityT[F, S, E, *]],
    partitioner: Partitioner[F, S, E, ID],
    applier: EventApplier[S, E]
) extends Repository[EntityT[F, S, E, *], ID, Alg] {
  def entityFor(id: ID): Alg[EntityT[F, S, E, *]] =
    interpretedAlgebra.mapK(partitioner).mapK(ReaderT.applyK(id))

  def runCommand[A](state: S, command: Command[Alg, A]): F[Folded[E, A]] =
    command.run(interpretedAlgebra).run(EventsFolder(state, applier))
}

object RepositoryT {
  implicit def instance[F[_], S, E, ID, Alg[_[_]]](implicit
      interpretedAlgebra: Alg[EntityT[F, S, E, *]],
      partitioner: Partitioner[F, S, E, ID],
      applier: EventApplier[S, E],
      functorK: FunctorK[Alg]
  ): RepositoryT[F, S, E, ID, Alg] = new RepositoryT
}
