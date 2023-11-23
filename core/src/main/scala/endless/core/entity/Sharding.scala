package endless.core.entity

import endless.core.protocol.{CommandProtocol, CommandSender}

/** `Sharding` represents the ability to access a specific entity in the sharded cluster, via a
  * handle of the entity algebra
  *
  * @tparam F
  *   context
  * @tparam ID
  *   id
  * @tparam Alg
  *   entity command handling algebra
  */
trait Sharding[F[_], ID, Alg[_[_]]] {

  /** Returns an instance of entity algebra `Alg` pointing to the entity with the specified ID
    * @param id
    *   entity ID
    * @return
    *   instance of `Alg` allowing to interact with the entity (issue commands)
    */
  def entityFor(id: ID): Alg[F]
}

object Sharding {
  implicit def apply[F[_], ID, Alg[_[_]]](implicit
      commandProtocol: CommandProtocol[ID, Alg],
      commandSender: CommandSender[F, ID]
  ): Sharding[F, ID, Alg] = (id: ID) => commandProtocol.clientFor(id)
}
