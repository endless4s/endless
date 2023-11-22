package endless.core.entity

import endless.core.protocol.{CommandProtocol, CommandSender}

/** `Repository` represents the entity repository and allows accessing an entity instance, i.e. an
  * instance of the corresponding algebra
  *
  * @tparam F
  *   context
  * @tparam ID
  *   id
  * @tparam Alg
  *   entity command handling algebra
  */
trait Repository[F[_], ID, Alg[_[_]]] {

  /** Returns an instance of entity algebra `Alg` pointing to the entity with the specified ID
    * @param id
    *   entity ID
    * @return
    *   instance of `Alg` allowing to interact with the entity (issue commands)
    */
  def entityFor(id: ID): Alg[F]
}

object Repository {
  implicit def apply[F[_], ID, Alg[_[_]]](implicit
      commandProtocol: CommandProtocol[ID, Alg],
      commandSender: CommandSender[F, ID]
  ): Repository[F, ID, Alg] = (id: ID) => commandProtocol.clientFor(id)
}
