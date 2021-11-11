package endless.core.interpret

import cats.laws.discipline.MonadTests
import cats.{Applicative, Eq, Functor, Monad}
import cats.tests.ListWrapper
import endless.core.interpret.EffectorT.{EffectorT, PassivationState}
import munit.DisciplineSuite
import org.typelevel.log4cats.Logger
import cats.tests.ListWrapper._
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import cats.syntax.flatMap._
import scala.concurrent.duration.{Duration, FiniteDuration}

class EffectorTSuite extends DisciplineSuite {
  type State = String
  type Value = Int

  implicit val F: Monad[ListWrapper] = ListWrapper.monad

  implicit def eqEffectorT[A: Eq]: Eq[EffectorT[ListWrapper, State, A]] =
    (x: EffectorT[ListWrapper, State, A], y: EffectorT[ListWrapper, State, A]) =>
      x.run(None, PassivationState.Disabled) == y.run(None, PassivationState.Disabled)

  implicit def arbitrary[F[_]: Monad, A](implicit
      F: Arbitrary[F[A]]
  ): Arbitrary[EffectorT[F, State, A]] =
    Arbitrary(F.arbitrary.map(EffectorT.liftF(_)))

  // check if resolves
  Functor[EffectorT[ListWrapper, State, *]]
  Applicative[EffectorT[ListWrapper, State, *]]
  Monad[EffectorT[ListWrapper, State, *]]

  checkAll(
    "EffectorT.MonadLaws",
    MonadTests[EffectorT[ListWrapper, State, *]].monad[Value, Value, Value]
  )

  test("passivationEnabler enables passivation") {
    val eff = EffectorT.unit[ListWrapper, State] >> EffectorT.passivationEnabler(
      Duration.Zero
    ) >> EffectorT.passivationDisabler >> EffectorT.passivationEnabler(Duration.Zero)

    assertEquals(
      eff.runS(None, PassivationState.Disabled).list.head,
      PassivationState.After(Duration.Zero)
    )
  }

  test("passivationDisabler disables passivation") {
    assertEquals(
      EffectorT.passivationDisabler
        .runS(None, PassivationState.After(Duration.Zero))
        .list
        .head,
      PassivationState.Disabled
    )
  }

  test("liftK is resolved by Logger auto-derive") {
    implicit val logger: Logger[ListWrapper] = new DummyTestLogger
    class SomeLoggingAlgebra[F[_]: Logger]
    new SomeLoggingAlgebra[EffectorT[ListWrapper, State, *]]
  }
}
