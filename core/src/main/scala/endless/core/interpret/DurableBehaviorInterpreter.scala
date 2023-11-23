package endless.core.interpret

import cats.Applicative
import endless.core.entity.DurableEntity
import endless.core.interpret.DurableEntityT.DurableEntityT

/** Interprets an algebra `Alg` expressed using `DurableEntity` in context `F` with `DurableEntityT`
  *
  * @tparam F
  *   effect type
  * @tparam S
  *   state
  * @tparam Alg
  *   entity algebra
  * @return
  *   interpreted entity algebra in context `F`
  */
trait DurableBehaviorInterpreter[F[_], S, Alg[_[_]]] {
  def apply(entity: DurableEntity[DurableEntityT[F, S, *], S]): F[Alg[DurableEntityT[F, S, *]]]
}

object DurableBehaviorInterpreter {

  /** Lifts a pure interpreter into an `DurableEntityInterpreter` (which is expressed in context
    * `F`)
    * @param pureInterpreter
    *   pure interpreter (i.e. it doesn't require any effect to create the algebra instance)
    * @tparam F
    *   effect type
    * @tparam S
    *   state
    * @tparam Alg
    *   entity algebra
    * @return
    *   entity algebra interpreter in context `F`
    */
  def lift[F[_]: Applicative, S, Alg[_[_]]](
      pureInterpreter: DurableEntity[DurableEntityT[F, S, *], S] => Alg[DurableEntityT[F, S, *]]
  ): DurableBehaviorInterpreter[F, S, Alg] =
    (entity: DurableEntity[DurableEntityT[F, S, *], S]) =>
      Applicative[F].pure(pureInterpreter(entity))
}
