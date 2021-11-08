package endless.core.typeclass.entity
import cats.data.EitherT
import cats.syntax.either._
import cats.{Id, Monad}
import org.scalacheck.Prop.forAll

class EntitySuite extends munit.ScalaCheckSuite {
  property("ifKnown with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity.ifKnown(identity)(Error.EntityNotFound).getOrElse(fail("empty state")),
        state
      )
    }
  }

  property("ifKnown with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity.ifKnown(identity)(Error.EntityNotFound).left.getOrElse(fail("non-empty state")),
      Error.EntityNotFound
    )
  }

  property("ifUnknown with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity.ifUnknown("foo")(_ => Error.EntityAlreadyExists).left.getOrElse(fail("empty state")),
        Error.EntityAlreadyExists
      )
    }
  }

  property("ifUnknown with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity.ifUnknown("foo")(_ => Error.EntityAlreadyExists).getOrElse(fail("non-empty state")),
      "foo"
    )
  }

  property("ifKnownF with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity.ifKnownF(identity)(Error.EntityNotFound).getOrElse(fail("empty state")),
        state
      )
    }
  }

  property("ifKnownF with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity.ifKnownF(identity)(Error.EntityNotFound).left.getOrElse(fail("non-empty state")),
      Error.EntityNotFound
    )
  }

  property("ifUnknownF with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity
          .ifUnknownF("foo")(_ => Error.EntityAlreadyExists)
          .left
          .getOrElse(fail("empty state")),
        Error.EntityAlreadyExists
      )
    }
  }

  property("ifUnknownF with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity.ifUnknownF("foo")(_ => Error.EntityAlreadyExists).getOrElse(fail("non-empty state")),
      "foo"
    )
  }

  property("ifKnownFE with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity
          .ifKnownFE(_.asRight[Error])(Error.EntityNotFound)
          .getOrElse(fail("empty state")),
        state
      )
    }
  }

  property("ifKnownFE with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity
        .ifKnownFE(_.asRight[Error])(Error.EntityNotFound)
        .left
        .getOrElse(fail("non-empty state")),
      Error.EntityNotFound
    )
  }

  property("ifUnknownFE with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity
          .ifUnknownFE("foo".asRight[Error])(_ => Error.EntityAlreadyExists)
          .left
          .getOrElse(fail("empty state")),
        Error.EntityAlreadyExists
      )
    }
  }

  property("ifUnknownFE with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity
        .ifUnknownFE("foo".asRight[Error])(_ => Error.EntityAlreadyExists)
        .getOrElse(fail("non-empty state")),
      "foo"
    )
  }

  property("ifKnownT with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity
          .ifKnownT(EitherT.pure[Id, Error](_))(Error.EntityNotFound)
          .getOrElse(fail("empty state")),
        state
      )
    }
  }

  property("ifKnownT with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity
        .ifKnownT(EitherT.pure[Id, Error](_))(Error.EntityNotFound)
        .left
        .getOrElse(fail("non-empty state")),
      Error.EntityNotFound
    )
  }

  property("ifUnknownT with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity
          .ifUnknownT(EitherT.pure[Id, Error]("foo"))(_ => Error.EntityAlreadyExists)
          .left
          .getOrElse(fail("empty state")),
        Error.EntityAlreadyExists
      )
    }
  }

  property("ifUnknownT with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity
        .ifUnknownT(EitherT.pure[Id, Error]("foo"))(_ => Error.EntityAlreadyExists)
        .getOrElse(fail("non-empty state")),
      "foo"
    )
  }

  type State = Int
  type Event = String
  sealed trait Error
  object Error {
    object EntityNotFound extends Error
    object EntityAlreadyExists extends Error
    case class Odd(state: State) extends Error
  }

  class TestEntity(state: Option[State]) extends Entity[Id, State, Event] {
    private val queue = new scala.collection.mutable.Queue[Event]
    def pure[A](x: A): Id[A] = x
    def read: Id[Option[State]] = state
    def write(event: Event, other: Event*): Id[Unit] = queue += event ++= other
    def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = implicitly[Monad[Id]].flatMap(fa)(f)
    def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] = implicitly[Monad[Id]].tailRecM(a)(f)
  }
}
