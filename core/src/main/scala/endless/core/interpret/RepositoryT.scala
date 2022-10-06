package endless.core.interpret

import cats.tagless.FunctorK
import cats.tagless.implicits._
import endless.core.entity.Repository
import endless.core.protocol.{CommandProtocol, CommandRouter}

/** `RepositoryT[F, ID, Alg]` is a data type implementing the `Repository[F, ID, Alg]` entity access
  * ability.
  *
  * It assembles the client command protocol together with the command router natural transformation
  * on `OutgoingCommand[*]` to deliver an instance of `Alg[F]` allowing to interact with the
  * specific entity in the cluster.
  *
  * @param commandProtocol
  *   command protocol used to issue commands to entities in the cluster
  * @param commandRouter
  *   command routing natural transformation
  * @tparam F
  *   context
  * @tparam ID
  *   entity id
  * @tparam Alg
  *   entity algebra
  */
final class RepositoryT[F[_], ID, Alg[_[_]]: FunctorK](implicit
    commandProtocol: CommandProtocol[Alg],
    commandRouter: CommandRouter[F, ID]
) extends Repository[F, ID, Alg] {
  def entityFor(id: ID): Alg[F] = commandProtocol.client.mapK(commandRouter.routerForID(id))
}

object RepositoryT {
  implicit def apply[F[_], ID, Alg[_[_]]: FunctorK](implicit
      commandProtocol: CommandProtocol[Alg],
      commandRouter: CommandRouter[F, ID]
  ): RepositoryT[F, ID, Alg] = new RepositoryT
}
