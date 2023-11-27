package endless.core.entity
import cats.data.EitherT
import cats.syntax.either.*
import cats.{Id, Monad}
import org.scalacheck.Prop.forAll

class StateReaderHelpersSuite extends munit.ScalaCheckSuite {
  property("ifKnown with state") {
    forAll { (state: State) =>
      val entity = new TestEntity(Some(state))
      assertEquals(
        entity.ifKnown(identity)(Error.EntityNotFound).getOrElse(fail("empty state")),
        state
      )
    }
  }

  test("ifKnown with no state") {
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

  test("ifUnknown with no state") {
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

  test("ifKnownF with no state") {
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

  test("ifUnknownF with no state") {
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

  test("ifKnownFE with no state") {
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

  test("ifUnknownFE with no state") {
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

  test("ifKnownT with no state") {
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

  test("ifUnknownT with no state") {
    val entity = new TestEntity(None)
    assertEquals(
      entity
        .ifUnknownT(EitherT.pure[Id, Error]("foo"))(_ => Error.EntityAlreadyExists)
        .getOrElse(fail("non-empty state")),
      "foo"
    )
  }

  type State = Int
  sealed trait Error
  object Error {
    object EntityNotFound extends Error
    object EntityAlreadyExists extends Error
    case class Odd(state: State) extends Error
  }

  private val idMonad = Monad[Id]
  class TestEntity(state: Option[State]) extends StateReaderHelpers[Id, State] {
    def read: Id[Option[State]] = state
    implicit val monad: Monad[Id] = idMonad
  }
}
