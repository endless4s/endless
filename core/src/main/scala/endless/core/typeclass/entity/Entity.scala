package endless.core.typeclass.entity

import cats.Monad
import endless.\/
import endless.core.typeclass.event.EventWriter
import cats.syntax.either._

/** `Entity[F, S, E]` is the ability to read an event-sourced entity state of type `S` and append
  * events of type `E` affecting this state, together with the ability to compose such dependent
  * effectful functions.
  *
  * These dual reader/writer abilities are what is needed to describe command handler behavior. When
  * interpreting code involving `Entity`, the final resulting value in the monadic chain is
  * typically understood as the reply, and all appended events are persisted to the journal
  * atomically by the runtime. Read always provides the up-to-date state however, thanks to event
  * folding happening within the interpreter, ensuring consistency throughout the chain.
  *
  * @tparam F
  *   context
  * @tparam S
  *   state
  * @tparam E
  *   event
  */
trait Entity[F[_], S, E] extends StateReader[F, S] with EventWriter[F, E] with Monad[F] {

  /** Convenience function which applies `fa` on the state if entity exists and wraps this in a
    * [[Right]], otherwise returns a [[Left]] with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnown[A, Error](fa: S => F[A])(ifUnknown: => Error): F[Error \/ A] =
    flatMap(read) {
      case Some(state) => map(fa(state))(_.asRight)
      case None        => pure(ifUnknown.asLeft)
    }

  /** Convenience function which invokes `fa` if entity doesn't yet exist and wraps this in a
    * [[Right]], otherwise calls `ifKnown` with the state and wraps this in a [[Left]].
    * @param fa
    *   success value when entity doesn't exist yet
    * @param ifKnown
    *   function to compute left value in case of existing entity
    */
  def ifUnknown[A, Error](fa: => F[A])(ifKnown: S => Error): F[Error \/ A] =
    flatMap(read) {
      case None        => map(fa)(_.asRight)
      case Some(state) => pure(ifKnown(state).asLeft)
    }
}
