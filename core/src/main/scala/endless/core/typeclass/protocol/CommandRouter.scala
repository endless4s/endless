package endless.core.typeclass.protocol

import cats.~>

/** `CommandRouter[F, ID]` provides a natural transformation to route an outgoing command to where
  * the entity resides and decode the reply as a simple value in the `F` context.
  * @tparam F
  *   context
  * @tparam ID
  *   entity ID
  */
trait CommandRouter[F[_], ID] {

  /** Returns a natural transformation to route an outgoing command to the entity with this
    * particular ID.
    * @param id
    *   entity ID
    */
  def routerForID(id: ID): OutgoingCommand[*] ~> F
}
