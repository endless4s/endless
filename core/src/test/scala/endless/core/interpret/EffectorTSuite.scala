package endless.core.interpret

import cats.laws.discipline.MonadTests
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.tests.ListWrapper
import cats.tests.ListWrapper._
import cats.{Applicative, Eq, Functor, Monad}
import endless.core.interpret.EffectorT.{EffectorT, PassivationState, _}
import munit.DisciplineSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.Duration

class EffectorTSuite extends DisciplineSuite {
  type State = String
  type Value = Int
  trait Alg[F[_]] {
    def universe: F[Int]
  }
  val algImpl = new Alg[ListWrapper] {
    def universe: ListWrapper[Int] = 42.pure[ListWrapper]
  }
  implicit val F: Monad[ListWrapper] = ListWrapper.monad

  implicit def eqEffectorT[A: Eq]: Eq[EffectorT[ListWrapper, State, Alg, A]] =
    (x: EffectorT[ListWrapper, State, Alg, A], y: EffectorT[ListWrapper, State, Alg, A]) =>
      x.run(Env(None, algImpl), PassivationState.Disabled) == y.run(
        Env(None, algImpl),
        PassivationState.Disabled
      )

  implicit def arbitrary[F[_]: Monad, A](implicit
      F: Arbitrary[F[A]]
  ): Arbitrary[EffectorT[F, State, Alg, A]] =
    Arbitrary(F.arbitrary.map(EffectorT.liftF(_)))

  // check if resolves
  Functor[EffectorT[ListWrapper, State, Alg, *]]
  Applicative[EffectorT[ListWrapper, State, Alg, *]]
  Monad[EffectorT[ListWrapper, State, Alg, *]]

  checkAll(
    "EffectorT.MonadLaws",
    MonadTests[EffectorT[ListWrapper, State, Alg, *]].monad[Value, Value, Value]
  )

  test("passivationEnabler enables passivation") {
    val eff = EffectorT.unit[ListWrapper, State, Alg] >> EffectorT.passivationEnabler(
      Duration.Zero
    ) >> EffectorT.passivationDisabler >> EffectorT.passivationEnabler(Duration.Zero)

    assertEquals(
      eff.runS(Env(None, algImpl), PassivationState.Disabled).list.head,
      PassivationState.After(Duration.Zero)
    )
  }

  test("passivationDisabler disables passivation") {
    assertEquals(
      EffectorT
        .passivationDisabler[ListWrapper, State, Alg]
        .runS(Env(None, algImpl), PassivationState.After(Duration.Zero))
        .list
        .head,
      PassivationState.Disabled
    )
  }

  test("algReader gives access to entity algebra") {
    assertEquals(
      EffectorT
        .entityAlgReader[ListWrapper, State, Alg]
        .runA(
          Env(None, algImpl),
          PassivationState.Disabled
        )
        .list
        .head
        .universe
        .list
        .head,
      42
    )
  }

  test("liftK is resolved by Logger auto-derive") {
    implicit val logger: Logger[ListWrapper] = new DummyTestLogger
    class SomeLoggingAlgebra[F[_]: Logger]
    new SomeLoggingAlgebra[EffectorT[ListWrapper, State, Alg, *]]
  }
}
