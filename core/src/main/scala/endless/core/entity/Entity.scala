package endless.core.entity

import cats.Monad
import cats.data.EitherT
import endless.\/
import endless.core.event.EventWriter
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
    * `Right`, otherwise returns a `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnown[Error, A](fa: S => A)(ifUnknown: => Error): F[Error \/ A] =
    ifKnownF[Error, A](s => pure(fa(s)))(ifUnknown)

  /** Convenience function which applies `fa` on the state if entity exists and wraps this in a
    * `Right`, otherwise returns a `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnownF[Error, A](fa: S => F[A])(ifUnknown: => Error): F[Error \/ A] =
    ifKnownFE[Error, A](s => map(fa(s))(_.asRight))(ifUnknown)

  /** Convenience function which applies `fa` on the state if entity exists, otherwise returns a
    * `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnownFE[Error, A](fa: S => F[Error \/ A])(ifUnknown: => Error): F[Error \/ A] =
    flatMap(read) {
      case Some(state) => fa(state)
      case None        => pure(ifUnknown.asLeft)
    }

  /** Convenience function which applies `fa` on the state if entity exists and unwraps EitherT
    * value, otherwise returns a `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnownT[Error, A](fa: S => EitherT[F, Error, A])(ifUnknown: => Error): F[Error \/ A] =
    ifKnownFE(s => fa(s).value)(ifUnknown)

  /** Convenience function which returns a in a `Right` if entity doesn't yet exist, otherwise calls
    * `ifKnown` with the state and wraps this in a `Left`.
    * @param fa
    *   success value when entity doesn't exist yet
    * @param ifKnown
    *   function to compute left value in case of existing entity
    */
  def ifUnknown[Error, A](a: => A)(ifKnown: S => Error): F[Error \/ A] =
    ifUnknownF[Error, A](pure(a))(ifKnown)

  /** Convenience function which invokes `fa` if entity doesn't yet exist and wraps this in a
    * `Right`, otherwise calls `ifKnown` with the state and wraps this in a `Left`.
    * @param fa
    *   success value when entity doesn't exist yet
    * @param ifKnown
    *   function to compute left value in case of existing entity
    */
  def ifUnknownF[Error, A](fa: => F[A])(ifKnown: S => Error): F[Error \/ A] =
    ifUnknownFE[Error, A](map(fa)(_.asRight))(ifKnown)

  /** Convenience function which invokes `fa` if entity doesn't yet exist and wraps this in a
    * `Right`, otherwise calls `ifKnown` with the state and wraps this in a `Left`.
    * @param fa
    *   success value when entity doesn't exist yet
    * @param ifKnown
    *   function to compute left value in case of existing entity
    */
  def ifUnknownFE[Error, A](fa: => F[Error \/ A])(ifKnown: S => Error): F[Error \/ A] =
    flatMap(read) {
      case None =>
        fa
      case Some(state) =>
        pure(ifKnown(state).asLeft)
    }

  /** Convenience function which applies `fa` on the state if entity exists and unwraps EitherT
    * value, otherwise returns a `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifUnknownT[Error, A](fa: => EitherT[F, Error, A])(ifUnknown: S => Error): F[Error \/ A] =
    ifUnknownFE(fa.value)(ifUnknown)

}
