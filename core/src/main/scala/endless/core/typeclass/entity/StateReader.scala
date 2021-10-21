package endless.core.typeclass.entity

/** `StateReader[F, S]` is the ability to read a value of type `S` from a shared environment in
  * context `F`, where that value is semantically understood as the current state of the entity.
  *
  * Note that this is equivalent to `Ask[F, S]` in
  * [[https://typelevel.org/cats-mtl/mtl-classes/ask.html cats mtl]].
  *
  * @tparam F
  *   context
  * @tparam S
  *   state
  */
trait StateReader[F[_], S] {

  /** Read the state from the environment, returns None if the entity doesn't yet exist
    * @return
    *   optional state in `F` context
    */
  def read: F[Option[S]]
}
