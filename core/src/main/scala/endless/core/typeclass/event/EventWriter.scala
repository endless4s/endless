package endless.core.typeclass.event

/** `EventWriter[F, S]` is the ability to append an event of type `E` to an event log in the context
  * `F`.
  *
  * Note that this is equivalent to `Tell[F, E]` in
  * [[https://typelevel.org/cats-mtl/mtl-classes/tell.html cats mtl]].
  *
  * @tparam E
  *   event
  */
trait EventWriter[F[_], E] {

  /** Append events to the event log in context `F`
    * @param event
    *   event
    * @param other
    *   more events
    * @return
    */
  def write(event: E, other: E*): F[Unit]
}
