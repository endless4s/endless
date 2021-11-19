package endless.core.typeclass.protocol

/** Encode/decode entity IDs to/from strings
  * @tparam ID
  *   entity id
  */
trait EntityIDCodec[ID] extends EntityIDEncoder[ID] with EntityIDDecoder[ID]

object EntityIDCodec {
  def apply[ID](encoder: ID => String, decoder: String => ID): EntityIDCodec[ID] =
    new EntityIDCodec[ID] {
      override def encode(id: ID): String = encoder(id)
      override def decode(str: String): ID = decoder(str)
    }
}
