package endless.core.interpret

import cats.tagless.FunctorK
import cats.~>
import org.typelevel.log4cats.Logger

/** `FunctorK[Logger]` instance to lift instances of `Logger[F]` into other monads, e.g. `EntityT`
  * and `EffectorT` monad transformers
  */
trait LoggerLiftingHelper {
  implicit val loggerFunctorK: FunctorK[Logger] = new FunctorK[Logger] {
    def mapK[F[_], G[_]](af: Logger[F])(fk: F ~> G): Logger[G] = af.mapK(fk)
  }
  implicit def loggerAutoDerive[F[_], G[_]](implicit af: Logger[F], fk: F ~> G): Logger[G] =
    loggerFunctorK.mapK(af)(fk)
}
