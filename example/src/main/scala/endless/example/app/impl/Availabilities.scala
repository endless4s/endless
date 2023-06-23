package endless.example.app.impl

import cats.Monad
import cats.effect.Async
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.show._
import endless.example.algebra.AvailabilityAlg
import org.typelevel.log4cats.Logger

import java.time.Instant
import scala.concurrent.duration._

trait Availabilities {
  implicit def alwaysAvailable[F[_]: Logger: Monad: Async]: AvailabilityAlg[F] =
    (time: Instant, passengerCount: Int) =>
      Logger[F].info(
        show"Checking if capacity is available for ${time.toString} and $passengerCount passengers"
      ) >> Async[F].sleep(500.millis) >> Logger[F].info(
        show"Availability confirmed for ${time.toString} and $passengerCount passengers"
      ) >> true
        .pure[F]
}
