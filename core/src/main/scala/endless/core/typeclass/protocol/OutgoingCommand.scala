package endless.core.typeclass.protocol

/** Represents an outgoing command. Embeds the binary payload and indicates the decoder to use for
  * the reply of type `R`
  * @tparam R
  *   reply
  */
trait OutgoingCommand[+R] {
  def payload: Array[Byte]
  def replyDecoder: Decoder[R]
}
