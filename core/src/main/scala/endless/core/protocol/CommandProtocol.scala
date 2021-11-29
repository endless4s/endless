package endless.core.protocol

/** `CommandProtocol` represents the serialization aspects of the entity.
  *
  * `client` provides an interpretation of the algebra with `OutgoingCommand[*]` for issuing
  * commands for each defined function in the algebra, and `server` is a decoder for an instance of
  * `IncomingCommand[F, Alg]` which represents an incoming command that can be directly run and
  * contains its own reply encoder as well.
  * @tparam Alg
  *   the entity algebra
  */
trait CommandProtocol[Alg[_[_]]] {

  /** Decoder for `IncomingCommand[F, Alg]` which can run the command and encode the reply
    * @tparam F
    *   context
    */
  def server[F[_]]: Decoder[IncomingCommand[F, Alg]]

  /** Instance of the entity algebra interpreted with `OutgoingCommand[*]` which contains the binary
    * payload and can decode the command reply
    */
  def client: Alg[OutgoingCommand[*]]
}
