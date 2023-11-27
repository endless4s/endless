package endless.core.interpret

import cats.laws.discipline.MonadTests
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.tests.ListWrapper
import cats.tests.ListWrapper.*
import cats.{Applicative, Eq, Functor, Monad}
import endless.core.interpret.DurableEntityT.*
import munit.DisciplineSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.*
import org.typelevel.log4cats.Logger

class DurableEntityTSuite extends DisciplineSuite {
  type SampleState = String
  type Value = Int
  val sampleState: SampleState = "foo"

  implicit val listWrapperMonad: Monad[ListWrapper] = ListWrapper.monad

  // check if resolves
  Functor[DurableEntityT[ListWrapper, SampleState, *]]
  Applicative[DurableEntityT[ListWrapper, SampleState, *]]
  Monad[DurableEntityT[ListWrapper, SampleState, *]]

  implicit def eq: Eq[DurableEntityT.State[SampleState]] =
    (x: State[SampleState], y: State[SampleState]) =>
      (x, y) match {
        case (State.None, State.None)               => true
        case (State.Existing(a), State.Existing(b)) => a === b
        case (State.Updated(a), State.Updated(b))   => a === b
        case (_, _)                                 => false
      }

  implicit def eqDurableEntityT[A: Eq]: Eq[DurableEntityT[ListWrapper, SampleState, A]] =
    (
        x: DurableEntityT[ListWrapper, SampleState, A],
        y: DurableEntityT[ListWrapper, SampleState, A]
    ) =>
      x.run(State.None) === y.run(State.None) &&
        x.run(State.Existing(sampleState)) === y.run(State.Existing(sampleState)) &&
        x.run(State.Updated(sampleState)) === y.run(State.Updated(sampleState))

  implicit def arbitrary[F[_]: Monad, A](implicit
      F: Arbitrary[F[A]]
  ): Arbitrary[DurableEntityT[F, SampleState, A]] = Arbitrary(
    F.arbitrary.map(
      DurableEntityT.stateWriter(sampleState)(Applicative[F]) >> DurableEntityT.liftF(_)
    )
  )

  checkAll(
    "DurableEntityT.MonadLaws",
    MonadTests[DurableEntityT[ListWrapper, SampleState, *]].monad[Value, Value, Value]
  )

  test("stateReader returns state") {
    assertEquals(
      DurableEntityT
        .stateReader[ListWrapper, SampleState]
        .runA(State.None)
        .list
        .head,
      None
    )
    assertEquals(
      DurableEntityT
        .stateReader[ListWrapper, SampleState]
        .runA(State.Existing(sampleState))
        .list
        .head,
      Some(sampleState)
    )
    assertEquals(
      DurableEntityT
        .stateReader[ListWrapper, SampleState]
        .runA(State.Updated(sampleState))
        .list
        .head,
      Some(sampleState)
    )
  }

  test("stateWriter writes state") {
    assertEquals(
      DurableEntityT
        .stateWriter[ListWrapper, SampleState](sampleState)
        .runS(State.None)
        .list
        .head,
      State.Updated(sampleState)
    )
  }

  test("stateModifier modifies state, if possible") {
    assertEquals(
      DurableEntityT
        .stateModifier[ListWrapper, SampleState](
          _.drop(1)
        )
        .runS(State.None)
        .list
        .head,
      State.None
    )
    assertEquals(
      DurableEntityT
        .stateModifier[ListWrapper, SampleState](
          _.drop(1)
        )
        .runS(State.Existing(sampleState))
        .list
        .head,
      State.Updated(sampleState.drop(1))
    )
    assertEquals(
      DurableEntityT
        .stateModifier[ListWrapper, SampleState](
          _.drop(1)
        )
        .runS(State.Updated(sampleState))
        .list
        .head,
      State.Updated(sampleState.drop(1))
    )
  }

  test("stateModifierF modifies state, if possible") {
    assertEquals(
      DurableEntityT
        .stateModifierF[ListWrapper, SampleState](state =>
          Applicative[ListWrapper].pure(state.drop(1))
        )
        .runS(State.None)
        .list
        .head,
      State.None
    )
    assertEquals(
      DurableEntityT
        .stateModifierF[ListWrapper, SampleState](state =>
          Applicative[ListWrapper].pure(state.drop(1))
        )
        .runS(State.Existing(sampleState))
        .list
        .head,
      State.Updated(sampleState.drop(1))
    )
    assertEquals(
      DurableEntityT
        .stateModifierF[ListWrapper, SampleState](state =>
          Applicative[ListWrapper].pure(state.drop(1))
        )
        .runS(State.Updated(sampleState))
        .list
        .head,
      State.Updated(sampleState.drop(1))
    )
  }

  test("unit returns unit") {
    assertEquals(
      DurableEntityT.unit[ListWrapper, SampleState].runA(State.None).list.head,
      ()
    )
  }

  test("liftK is resolved by Logger auto-derive") {
    implicit val logger: Logger[ListWrapper] = new DummyTestLogger
    class SomeLoggingAlgebra[F[_]: Logger]
    new SomeLoggingAlgebra[DurableEntityT[ListWrapper, SampleState, *]]
  }
}
