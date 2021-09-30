package endless.core.interpret

import endless.core.data.Folded
import endless.core.typeclass.protocol.CommandRouter
import cats.tagless.FunctorK
import cats.tagless.implicits._
import endless.core.data.EventsFolder
import endless.core.typeclass.entity.Repository
import endless.core.typeclass.event.EventApplier
import endless.core.typeclass.protocol.{CommandProtocol, IncomingCommand}

final class RepositoryT[F[_], S, E, ID, Alg[_[_]]: FunctorK](implicit
    entity: Alg[EntityT[F, S, E, *]],
    commandProtocol: CommandProtocol[Alg],
    commandRouter: CommandRouter[F, ID],
    eventApplier: EventApplier[S, E]
) extends Repository[F, ID, Alg] {
  def entityFor(id: ID): Alg[F] = commandProtocol.client.mapK(commandRouter.routerForID(id))

  def runCommand(
      state: S,
      command: IncomingCommand[EntityT[F, S, E, *], Alg]
  ): F[Folded[E, command.Reply]] =
    command.runWith(entity).run(EventsFolder(state, eventApplier))
}

object RepositoryT {
  implicit def apply[F[_], S, E, ID, Alg[_[_]]: FunctorK](implicit
      entity: Alg[EntityT[F, S, E, *]],
      commandProtocol: CommandProtocol[Alg],
      commandRouter: CommandRouter[F, ID],
      applier: EventApplier[S, E]
  ): RepositoryT[F, S, E, ID, Alg] = new RepositoryT
}
