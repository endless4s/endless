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
trait BehaviorInterpreter[F[_], S, E, Alg[_[_]]] {
  def apply(entity: Entity[EntityT[F, S, E, *], S, E]): F[Alg[EntityT[F, S, E, *]]]
}

object BehaviorInterpreter {

  /** Lifts a pure interpreter into an `EntityInterpreter` (which is expressed in context `F`)
    * @param pureInterpreter
    *   pure interpreter (i.e. it doesn't require any effect to create the algebra instance)
    * @tparam F
    *   effect type
    * @tparam S
    *   state
    * @tparam E
    *   event
    * @tparam Alg
    *   entity algebra
    * @return
    *   entity algebra interpreter in context `F`
    */
  def lift[F[_]: Applicative, S, E, Alg[_[_]]](
      pureInterpreter: Entity[EntityT[F, S, E, *], S, E] => Alg[EntityT[F, S, E, *]]
  ): BehaviorInterpreter[F, S, E, Alg] = (entity: Entity[EntityT[F, S, E, *], S, E]) =>
    Applicative[F].pure(pureInterpreter(entity))
}
