package endless.core.entity

import cats.effect.IO
import cats.effect.kernel.Ref
import munit.ScalaCheckEffectSuite
import org.scalacheck.effect.PropF._
import scala.concurrent.duration._

class EffectorSuite extends munit.CatsEffectSuite with ScalaCheckEffectSuite {
  test("ifKnown with state") {
    forAllF { (state: State) =>
      for {
        sideEffected <- Ref.of[IO, Boolean](false)
        effector <- Effector.apply(fooIO, Some(state))
        _ <- effector.ifKnown(_ => sideEffected.set(true))
        _ <- assertIO(sideEffected.get, true)
      } yield ()
    }
  }

  test("ifKnown without state") {
    for {
      sideEffected <- Ref.of[IO, Boolean](false)
      effector <- Effector.apply(fooIO, Option.empty[State])
      _ <- effector.ifKnown(_ => sideEffected.set(true))
      _ <- assertIO(sideEffected.get, false)
    } yield ()
  }

  test("read allows to read state") {
    forAllF { (state: State) =>
      for {
        effector <- Effector.apply(fooIO, Some(state))
        _ <- assertIO(effector.read, Some(state))
      } yield ()
    }
  }

  test("enablePassivation sets passivation state") {
    for {
      effector <- Effector.apply(fooIO, Option.empty[State])
      _ <- effector.enablePassivation(1.second)
      _ <- assertIO(effector.passivationState, Effector.PassivationState.After(1.second))
    } yield ()

  }

  test("disablePassivation sets passivation state to disabled") {
    for {
      effector <- Effector.apply(fooIO, Option.empty[State])
      _ <- effector.disablePassivation
      _ <- assertIO(effector.passivationState, Effector.PassivationState.Disabled)
    } yield ()

  }

  test("read does not affect passivation state") {
    for {
      effector <- Effector.apply(fooIO, Option.empty[State])
      _ <- effector.read
      _ <- assertIO(effector.passivationState, Effector.PassivationState.Unchanged)
    } yield ()

  }

  test("self allows to call entity algebra") {
    for {
      effector <- Effector.apply(fooIO, Option.empty[State])
      _ <- effector.self.foo
    } yield ()
  }

  type State = Int
  type Event = String
  trait Alg[F[_]] {
    def foo: F[Unit]
  }
  val fooIO = new Alg[IO] {
    def foo: IO[Unit] = IO(())
  }
}
