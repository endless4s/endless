package endless.core.interpret

import cats.data.ReaderT
import cats.{Functor, ~>}
import org.typelevel.log4cats.Logger

/** Helpers to lift instances of `Logger[F]` into [[EntityT]] and [[ReaderT]] monad transformers,
  * required for execution as the library makes uses of [[Logger]] for some built-in logging
  */
trait LoggerLiftingHelpers {
  implicit def loggerForEntityT[F[_]: Functor, S, E](implicit
      logger: Logger[F]
  ): Logger[EntityT[F, S, E, *]] = logger.mapK(new (F ~> EntityT[F, S, E, *]) {
    override def apply[A](fa: F[A]): EntityT[F, S, E, A] = EntityT.liftF[F, S, E, A](fa)
  })

  implicit def loggerForReaderT[F[_], S](implicit logger: Logger[F]): Logger[ReaderT[F, S, *]] =
    logger.mapK(new (F ~> ReaderT[F, S, *]) {
      override def apply[A](fa: F[A]): ReaderT[F, S, A] = ReaderT.liftF(fa)
    })
}
