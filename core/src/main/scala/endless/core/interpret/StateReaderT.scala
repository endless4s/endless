package endless.core.interpret

import cats.Applicative
import cats.data.ReaderT
import endless.core.typeclass.entity.StateReader

/** `StateReaderT` lifts `cats.data.ReaderT.ask` as `read` to equip `F` with the ability to read
  * from a shared environment
  */
object StateReaderT {
  trait StateReaderLift[G[_], F[_], S] extends StateReader[G, S]

  implicit def instance[F[_]: Applicative, S]: StateReaderLift[ReaderT[F, Option[S], *], F, S] =
    new StateReaderLift[ReaderT[F, Option[S], *], F, S] {
      override def read: ReaderT[F, Option[S], Option[S]] = ReaderT.ask
    }
}
