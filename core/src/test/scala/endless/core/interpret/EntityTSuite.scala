package endless.core.interpret

import cats.data.{Chain, NonEmptyChain}
import cats.laws.discipline.{FunctorTests, MonadTests}
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.tests.ListWrapper
import cats.tests.ListWrapper.*
import cats.{Applicative, Eq, Functor, Monad}
import endless.core.event.EventApplier
import endless.core.interpret.EntityT.*
import munit.DisciplineSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.*
import org.typelevel.log4cats.Logger

class EntityTSuite extends DisciplineSuite {
  type State = Chain[Event] // state simply accumulates events for test purposes
  type Event = String
  type Value = Int
  val event1 = "event 1"
  val event2 = "event 2"
  val event3 = "event 3"
  implicit val eventApplier: EventApplier[State, Event] = (state: Option[State], event: Event) =>
    state.map(_ :+ event).asRight

  implicit val F: Monad[ListWrapper] = ListWrapper.monad // ListWrapper is cat's base testing effect

  implicit def eqEntityT[A: Eq]: Eq[EntityT[ListWrapper, State, Event, A]] =
    (x: EntityT[ListWrapper, State, Event, A], y: EntityT[ListWrapper, State, Event, A]) =>
      x.run(Some(Chain.empty)) === y.run(Some(Chain.empty))

  implicit def arbitrary[F[_]: Monad, A](implicit
      F: Arbitrary[F[A]]
  ): Arbitrary[EntityT[F, State, Event, A]] =
    Arbitrary(
      F.arbitrary.map(
        EntityT.writer[F, State, Event](NonEmptyChain(event1, event2)) >> EntityT
          .writer[F, State, Event](NonEmptyChain.one(event3)) >> EntityT.liftF(_)
      )
    )

  // check if resolves
  Functor[EntityT[ListWrapper, State, Event, *]]
  Applicative[EntityT[ListWrapper, State, Event, *]]
  Monad[EntityT[ListWrapper, State, Event, *]]

  checkAll(
    "EntityT.MonadLaws",
    MonadTests[EntityT[ListWrapper, State, Event, *]].monad[Value, Value, Value]
  )

  checkAll(
    "EntityT.FunctorLaws for direct map def",
    FunctorTests[EntityT[ListWrapper, State, Event, *]](
      using
      new Functor[EntityT[ListWrapper, State, Event, *]] {
        override def map[A, B](fa: EntityT[ListWrapper, State, Event, A])(
            f: A => B
        ): EntityT[ListWrapper, State, Event, B] = fa.map(f)
      }
    )
      .functor[Value, Value, Value]
  )

  test("writer appends events") {
    val w1 = EntityT.writer[ListWrapper, State, Event](NonEmptyChain.one(event1))
    val w2 = w1 >> EntityT.writer(NonEmptyChain(event2, event3))
    assert(w2 === EntityT.writer(NonEmptyChain(event1, event2, event3)))
  }

  test("reader folds state") {
    val result = (EntityT.writer[ListWrapper, State, Event](
      NonEmptyChain(event1, event2, event3)
    ) >> EntityT.reader).run(Option(Chain.empty)).list.head
    result match {
      case Right((events, Some(folded))) =>
        assert(events === Chain(event1, event2, event3))
        assert(folded === Chain(event1, event2, event3))
      case _ => fail("reader failed")
    }
  }

  test("failing folder fails when reader is involved") {
    val result = (EntityT
      .writer[ListWrapper, State, Event](NonEmptyChain.one(event1)) >> EntityT.reader)
      .run(Some(Chain.empty))(using (_: Option[State], _: Event) => "error".asLeft)
      .list
      .head
    result match {
      case Left("error") => ()
      case _             => fail("reader failed")
    }
  }

  test("liftK is resolved by Logger auto-derive") {
    implicit val logger: Logger[ListWrapper] = new DummyTestLogger
    class SomeLoggingAlgebra[F[_]: Logger]
    new SomeLoggingAlgebra[EntityT[ListWrapper, State, Event, *]]
  }
}
