package endless.core.entity

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import endless.\/

/** Set of convenience functions augmenting `StateReader` (that assume a `Monad` instance exists for
  * `F`)
  * @tparam F
  *   context
  * @tparam S
  *   state
  */
trait StateReaderHelpers[F[_], S] extends StateReader[F, S] {
  implicit def monad: Monad[F]

  /** Convenience function which applies `fa` on the state if entity exists and wraps this in a
    * `Right`, otherwise returns a `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnown[Error, A](fa: S => A)(ifUnknown: => Error): F[Error \/ A] =
    ifKnownF[Error, A](s => fa(s).pure)(ifUnknown)

  /** Convenience function which applies `fa` on the state if entity exists and wraps this in a
    * `Right`, otherwise returns a `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnownF[Error, A](fa: S => F[A])(ifUnknown: => Error): F[Error \/ A] =
    ifKnownFE[Error, A](s => fa(s).map(_.asRight))(ifUnknown)

  /** Convenience function which applies `fa` on the state if entity exists, otherwise returns a
    * `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnownFE[Error, A](fa: S => F[Error \/ A])(ifUnknown: => Error): F[Error \/ A] =
    ifKnownElse(fa)(ifUnknown.asLeft[A].pure)

  /** Convenience function which applies `fa` on the state if entity exists and unwraps EitherT
    * value, otherwise returns a `Left` with the provided error value.
    * @param fa
    *   function to apply on state
    * @param ifUnknown
    *   left value in case of missing entity
    */
  def ifKnownT[Error, A](fa: S => EitherT[F, Error, A])(ifUnknown: => Error): F[Error \/ A] =
    ifKnownFE(s => fa(s).value)(ifUnknown)

  /** Convenience function which applies `fa` on the state if entity exists, otherwise returns a
    * default value
    *
    * @param fa
    *   function to apply on state
    * @param ifUnknownF
    *   value in case of missing entity in `F` context
    */
  def ifKnownElse[A](fa: S => F[A])(ifUnknownF: => F[A]): F[A] =
    read.flatMap {
      case Some(state) => fa(state)
      case None        => ifUnknownF
    }

  /** Convenience function which returns a in a `Right` if entity doesn't yet exist, otherwise calls
    * `ifKnown` with the state and wraps this in a `Left`.
    * @param fa
    *   success value when entity doesn't exist yet
    * @param ifKnown
    *   function to compute left value in case of existing entity
    */
  def ifUnknown[Error, A](a: => A)(ifKnown: S => Error): F[Error \/ A] =
    ifUnknownF[Error, A](a.pure)(ifKnown)

  /** Convenience function which invokes `fa` if entity doesn't yet exist and wraps this in a
    * `Right`, otherwise calls `ifKnown` with the state and wraps this in a `Left`.
    * @param fa
    *   success value when entity doesn't exist yet
    * @param ifKnown
    *   function to compute left value in case of existing entity
    */
  def ifUnknownF[Error, A](fa: => F[A])(ifKnown: S => Error): F[Error \/ A] =
    ifUnknownFE[Error, A](fa.map(_.asRight))(ifKnown)

  /** Convenience function which invokes `fa` if entity doesn't yet exist and wraps this in a
    * `Right`, otherwise calls `ifKnown` with the state and wraps this in a `Left`.
    * @param fa
    *   success value when entity doesn't exist yet
    * @param ifKnown
    *   function to compute left value in case of existing entity
    */
  def ifUnknownFE[Error, A](fa: => F[Error \/ A])(ifKnown: S => Error): F[Error \/ A] =
    ifUnknownElse(fa)(state => ifKnown(state).asLeft[A].pure)

  /** Convenience function which invokes `fa` if entity doesn't yet exist, otherwise calls ,
    * `ifKnown` with the the state and wraps this in a `Left`.
    * @param fa
    *   value wrapped in `EitherT` when entity doesn't exist yet
    * @param ifKnown
    *   function to compute left value in case of existing entity
    */
  def ifUnknownT[Error, A](fa: => EitherT[F, Error, A])(ifKnown: S => Error): F[Error \/ A] =
    ifUnknownFE(fa.value)(ifKnown)

  /** Convenience function which returns a value `fa` in `F` context, otherwise calls `ifKnown` with
    * the state
    *
    * @param fa
    *   value in case of missing entity in `F` context
    * @param ifKnown
    *   function to apply on state
    */
  def ifUnknownElse[A](fa: => F[A])(ifKnown: S => F[A]): F[A] =
    read.flatMap {
      case Some(state) => ifKnown(state)
      case None        => fa
    }
}
