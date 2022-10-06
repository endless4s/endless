package endless.core.entity

/** `StateWriter[F, S]` is the ability to write a value of type `S`, where that value is
  * semantically understood as the new state of the entity.
  *
  * @tparam F
  *   context
  * @tparam S
  *   state
  */
trait StateWriter[F[_], S] {

  /** Write the entity state
    * @param s
    *   entity state
    * @return
    *   unit in `F` context
    */
  def write(s: S): F[Unit]

  /** Modify the entity state with the given function
    * @param f
    *   state modifier
    * @return
    *   unit in `F` context
    */
  def modify(f: S => S): F[Unit]

  /** Modify the entity state with the given function expressed in `F` context
    * @param f
    *   state modifier in `F` context
    * @return
    *   unit in `F` context
    */
  def modifyF(f: S => F[S]): F[Unit]
}
