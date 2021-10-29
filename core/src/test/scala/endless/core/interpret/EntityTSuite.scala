package endless.core.interpret

import cats.data.{Chain, NonEmptyChain}
import cats.laws.discipline.MonadTests
import cats.syntax.either._
import cats.syntax.eq._
import cats.tests.ListWrapper
import cats.tests.ListWrapper._
import cats.{Applicative, Eq, Functor, Monad}
import endless.core.data.EventsFolder
import endless.core.interpret.EntityT._
import endless.core.typeclass.event.EventApplier
import munit.DisciplineSuite
import org.scalacheck.Arbitrary._
import org.scalacheck.Arbitrary
import cats.syntax.flatMap._

class EntityTSuite extends DisciplineSuite {
  type State = Chain[Event] // state simply accumulates events for test purposes
  type Event = String
  type Value = Int
  val event1 = "event 1"
  val event2 = "event 2"
  val event3 = "event 3"
  val eventApplier: EventApplier[State, Event] = (state: Option[State], event: Event) =>
    state.map(_ :+ event).asRight
  val folder = EventsFolder(Some(Chain.empty), eventApplier)

  implicit val F: Monad[ListWrapper] = ListWrapper.monad // ListWrapper is cat's base testing effect

  implicit def eqEntityT[A: Eq]: Eq[EntityT[ListWrapper, State, Event, A]] =
    (x: EntityT[ListWrapper, State, Event, A], y: EntityT[ListWrapper, State, Event, A]) =>
      x.run(folder) === y.run(folder)

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

  test("writer appends events") {
    val w1 = EntityT.writer[ListWrapper, State, Event](NonEmptyChain.one(event1))
    val w2 = w1 >> EntityT.writer(NonEmptyChain(event2, event3))
    assert(w2 === EntityT.writer(NonEmptyChain(event1, event2, event3)))
  }

  test("reader folds state") {
    val List(Right((events, Some(folded)))) = (EntityT
      .writer[ListWrapper, State, Event](
        NonEmptyChain(event1, event2, event3)
      ) >> EntityT.reader)
      .run(folder)
      .list

    assert(events === Chain(event1, event2, event3))
    assert(folded === Chain(event1, event2, event3))
  }

  test("failing folder fails when reader is involved") {
    val List(Left(error)) = (EntityT
      .writer[ListWrapper, State, Event](NonEmptyChain.one(event1)) >> EntityT.reader)
      .run(EventsFolder(Some(Chain.empty), (_: Option[State], _: Event) => "error".asLeft))
      .list

    assert(error === "error")
  }
}
