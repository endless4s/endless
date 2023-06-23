package endless.runtime.pekko.data

/** Internal carrier data type for entity replies
  * @param payload
  *   binary payload
  */
@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
final case class Reply(payload: Array[Byte])
