package endless.core.entity

/** `SideEffect[F, S, Alg]` represents a side-effect applied in context `F`. The side-effect is
  * triggered just after events persistence, and is interpreted with `Async` in order to allow for
  * asynchronous processes. The passed `Effector` can be used to access entity state and algebra and
  * to control passivation.
  * @tparam F
  *   effect type
  * @tparam S
  *   entity state
  * @tparam Alg
  *   entity algebra
  */
trait SideEffect[F[_], S, Alg[_[_]]] {
  def apply(effector: Effector[F, S, Alg]): F[Unit]
}
