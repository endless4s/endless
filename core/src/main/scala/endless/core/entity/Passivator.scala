package endless.core.entity

import endless.core.entity.Effector.PassivationState

import scala.concurrent.duration.{Duration, FiniteDuration}

/** `Passivator` represents the ability to "passivate" an entity, i.e. flush out an entity from the
  * system temporarily
  * @tparam F
  *   context
  */
trait Passivator[F[_]] {

  /** Schedule entity passivation after the specified delay - if zero (default), passivate
    * immediately
    * @param after
    *   delay before passivation
    */
  def enablePassivation(after: FiniteDuration = Duration.Zero): F[Unit]

  /** Disable scheduled passivation (note that entities automatically recover when receiving a
    * message)
    */
  def disablePassivation: F[Unit]

  /** Get the current passivation state
    */
  def passivationState: F[PassivationState]
}
