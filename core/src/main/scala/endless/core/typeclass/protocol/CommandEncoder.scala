package endless.core.typeclass.protocol

trait CommandEncoder[C] {
  def encode(a: C): OutgoingCommand[C]
}
