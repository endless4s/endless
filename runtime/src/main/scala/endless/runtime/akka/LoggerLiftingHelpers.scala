package endless.runtime.akka

import cats.data.ReaderT
import cats.{Functor, ~>}
import endless.core.interpret.EntityT
import org.typelevel.log4cats.Logger

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
