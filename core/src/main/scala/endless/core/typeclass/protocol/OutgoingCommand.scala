package endless.core.typeclass.protocol

trait OutgoingCommand[+R] {
  def payload: Array[Byte]
  def replyDecoder: Decoder[R]
}
