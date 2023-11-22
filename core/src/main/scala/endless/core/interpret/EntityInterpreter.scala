package endless.core.interpret

import cats.Applicative
import endless.core.entity.Entity

/** Interprets an algebra `Alg` expressed using `Entity` in context `F` with `EntityT`
  *
  * @tparam F
  *   effect type
  * @tparam S
  *   state
  * @tparam E
  *   event
  * @tparam Alg
  *   entity algebra
  * @return
  *   interpreted entity algebra in context `F`
  */
trait EntityInterpreter[F[_], S, E, Alg[_[_]]] {
  def apply(entity: Entity[EntityT[F, S, E, *], S, E]): F[Alg[EntityT[F, S, E, *]]]
}

object EntityInterpreter {
  def pure[F[_]: Applicative, S, E, Alg[_[_]]](
      f: Entity[EntityT[F, S, E, *], S, E] => Alg[EntityT[F, S, E, *]]
  ): EntityInterpreter[F, S, E, Alg] = (entity: Entity[EntityT[F, S, E, *], S, E]) =>
    Applicative[F].pure(f(entity))
}
