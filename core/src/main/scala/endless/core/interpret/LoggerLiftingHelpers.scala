package endless.core.interpret

import cats.data.ReaderWriterStateT
import cats.{Applicative, Functor, ~>}
import endless.core.interpret.EffectorT.{EffectorT, PassivationState}
import org.typelevel.log4cats.Logger

/** Helpers to lift instances of `Logger[F]` into `EntityT` and `EffectorT` monad transformers,
  * required for execution as the library makes uses of `Logger` for some built-in logging
  */
trait LoggerLiftingHelpers {
  implicit def loggerForEntityT[F[_]: Functor, S, E](implicit
      logger: Logger[F]
  ): Logger[EntityT[F, S, E, *]] = logger.mapK(new (F ~> EntityT[F, S, E, *]) {
    override def apply[A](fa: F[A]): EntityT[F, S, E, A] = EntityT.liftF[F, S, E, A](fa)
  })

  implicit def loggerForEffectorT[F[_]: Applicative, S](implicit
      logger: Logger[F]
  ): Logger[EffectorT[F, S, *]] =
    logger.mapK(new (F ~> EffectorT[F, S, *]) {
      override def apply[A](fa: F[A]): EffectorT[F, S, A] =
        ReaderWriterStateT.liftF[F, Option[S], Unit, PassivationState, A](fa)
    })
}
