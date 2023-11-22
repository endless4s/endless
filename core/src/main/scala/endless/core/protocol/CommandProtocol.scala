package endless.core.protocol

/** `CommandProtocol` represents the transport aspects of the entity cluster.
  *
  * `client` provides an implementation of the algebra which sends commands for each defined
  * function in the algebra and decodes the reply, and `server` is a decoder for instances of
  * `IncomingCommand[F, Alg]` that represent incoming commands that can be run and contain their own
  * reply encoder.
  * @tparam Alg
  *   the entity algebra
  */
trait CommandProtocol[ID, Alg[_[_]]] {

  /** Decoder for `IncomingCommand[F, Alg]` which can run the command and encode the reply
    * @tparam F
    *   context
    */
  def server[F[_]]: Decoder[IncomingCommand[F, Alg]]

  /** Returns an instance of entity algebra that translates calls into commands, sends them via the
    * `CommandSender` instance in implicit scope, and decodes the reply (implements an RPC-like
    * client).
    */
  def clientFor[F[_]](id: ID)(implicit sender: CommandSender[F, ID]): Alg[F]
}

object CommandProtocol {

  /** Helper function that sends a command to the entity with the specified ID using the sender in
    * implicit scope.
    *
    * @param id
    *   entity ID
    * @param command
    *   command
    * @param sender
    *   command sender
    * @tparam F
    *   context
    * @tparam R
    *   reply type
    * @return
    */
  def sendCommand[F[_], ID, R](id: ID, command: OutgoingCommand[R])(implicit
      sender: CommandSender[F, ID]
  ): F[R] = sender.senderForID(id)(command)
}
