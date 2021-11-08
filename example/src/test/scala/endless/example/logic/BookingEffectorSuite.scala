package endless.example.logic

import cats.effect.IO
import cats.syntax.show._
import endless.core.interpret.EffectorT
import endless.core.interpret.EffectorT._
import endless.example.data.Booking
import org.scalacheck.effect.PropF._
import org.typelevel.log4cats.testing.TestingLogger
import scala.concurrent.duration._

//#example
class BookingEffectorSuite
    extends munit.CatsEffectSuite
    with munit.ScalaCheckEffectSuite
    with Generators {
  implicit private val logger: TestingLogger[IO] = TestingLogger.impl[IO]()
  private val effector = BookingEffector(EffectorT.instance[IO, Booking])

  test("empty state log") {
    effector
      .runA(None)
      .flatMap(_ => assertIO(logger.logged.map(_.map(_.message).head), "State is empty"))
  }

  test("some state log") {
    forAllF { booking: Booking =>
      effector
        .runA(Some(booking))
        .flatMap(_ =>
          assertIOBoolean(logger.logged.map(_.map(_.message).contains(show"State is now $booking")))
        )
    }
  }

  test("some state passivate after one hour") {
    forAllF { booking: Booking =>
      assertIO(effector.runS(Some(booking)), PassivationState.After(1.hour))
    }
  }

  test("passivate immediately when cancelled") {
    forAllF { booking: Booking =>
      assertIO(
        effector.runS(Some(booking.copy(cancelled = true))),
        PassivationState.After(Duration.Zero)
      )
    }
  }
}
//#example