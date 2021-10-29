package endless.core.typeclass.entity

/** `Effector` represents the ability to read the state of the entity and perform a possible
  * passivation side-effect
  * @tparam F
  *   context
  * @tparam S
  *   state
  */
trait Effector[F[_], S] extends StateReader[F, S] with Passivator[F]
