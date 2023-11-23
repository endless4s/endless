# Side-effect

```scala
trait SideEffect[F[_], S, Alg[_[_]]] {
  def apply(effector: Effector[F, S, Alg]): F[Unit]
}
```

@scaladoc[SideEffect](endless.core.entity.SideEffect) is parametrized with the following type parameters:

 - `F[_]`: abstract effectful context `F` encapsulating all values, e.g. `IO[*]`
 - `Alg[_[_]]`: entity algebra, allowing "back-interaction" with the entity itself (e.g. for at least once process definition, see note in @ref:[Effector](effector.md))
 - `S`: entity state, e.g. @github[Booking](/example/src/main/scala/endless/example/data/Booking.scala)

It represents a side-effect that is triggered just after events persistence, and interpreted with `Async` to allow for asynchronicity. The passed @ref:[Effector](effector.md) instance can be used to access entity state, chain further interactions with the entity itself and to control passivation.