package endless.core.typeclass.protocol

import cats.~>

trait CommandRouter[F[_], ID] {
  def routerForID(id: ID): OutgoingCommand[*] ~> F
}
