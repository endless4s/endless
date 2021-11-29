package endless.core.entity

import cats.Id
import org.scalacheck.Prop.forAll

import scala.concurrent.duration.FiniteDuration

class EffectorSuite extends munit.ScalaCheckSuite {
  property("ifKnown with state") {
    forAll { (state: State) =>
      var sideEffected = false
      val effector = new TestEffector(Some(state))
      effector.ifKnown(_ => sideEffected = true)
      assert(sideEffected)
    }
  }

  property("ifKnown without state") {
    var sideEffected = false
    val effector = new TestEffector(None)
    effector.ifKnown(_ => sideEffected = true)
    assert(!sideEffected)
  }

  type State = Int
  type Event = String
  trait Alg[F[_]] {
    def foo: F[Unit]
  }

  class TestEffector(state: Option[State]) extends Effector[Id, State, Alg] {
    def self: Id[Alg[Id]] = new Alg[Id] {
      def foo: Id[Unit] = ()
    }
    def enablePassivation(after: FiniteDuration): Id[Unit] = ()
    def disablePassivation: Id[Unit] = ()
    def read: Id[Option[State]] = state
  }
}
