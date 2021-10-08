package endless.core.interpret

import cats.Applicative
import cats.data.ReaderT
import endless.core.typeclass.entity.StateReader

object StateReaderT {
  trait StateReaderLift[G[_], F[_], S] extends StateReader[G, S]

  implicit def instance[F[_]: Applicative, S]: StateReaderLift[ReaderT[F, S, *], F, S] =
    new StateReaderLift[ReaderT[F, S, *], F, S] {
      override def read: ReaderT[F, S, S] = ReaderT.ask
    }
}
