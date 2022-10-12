package endless.core.interpret

import cats.data.Chain
import cats.effect.{Clock, Concurrent, GenSpawn, GenTemporal, IO, MonadCancel, Sync, Temporal}
import cats.effect.laws.AsyncTests
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.{MiniInt, MonadTests}
import cats.laws.discipline.eq._
import cats.syntax.all._
import cats.derived.auto.eq._
import cats.derived.auto.order._
import cats.effect.kernel.{Async, GenConcurrent}
import cats.effect.testkit.TestInstances
import cats.tagless.FunctorK
import cats.tests.ListWrapper
import cats.{Applicative, Eq, Functor, Monad, Order, ~>}
import endless.core.interpret.EffectorT._
import munit.DisciplineSuite
import org.scalacheck.{Arbitrary, Cogen, Prop}
import org.typelevel.log4cats.Logger

import scala.language.implicitConversions
import scala.concurrent.duration._

class EffectorTSuite extends DisciplineSuite with TestInstances {
  type State = MiniInt
  type Value = Int
  trait Alg[F[_]] {
    def universe: F[Int]
  }
  implicit val functorKAlg: FunctorK[Alg] = new FunctorK[Alg] {
    def mapK[F[_], G[_]](af: Alg[F])(fk: F ~> G): Alg[G] = new Alg[G] {
      def universe: G[Value] = fk(af.universe)
    }
  }
  def algImpl[F[_]: Applicative]: Alg[F] = new Alg[F] {
    def universe: F[Int] = 42.pure[F]
  }
  implicit val listWrapperMonad: Monad[ListWrapper] = ListWrapper.monad

  implicit val ticker: Ticker = Ticker()

  implicit def eqEffectorT[F[_]: Monad, A: Eq](implicit
      eqF: Eq[F[(Unit, PassivationState, A)]]
  ): Eq[EffectorT[F, State, Alg, A]] =
    (x: EffectorT[F, State, Alg, A], y: EffectorT[F, State, Alg, A]) =>
      x.run(None, algImpl[F], PassivationState.Disabled) === y.run(
        None,
        algImpl[F],
        PassivationState.Disabled
      )

  implicit def ordEffectorTIO(implicit
      ticker: Ticker
  ): Order[EffectorT[IO, State, Alg, FiniteDuration]] = {
    implicit val passivationStateOrder: Order[PassivationState] =
      (x: PassivationState, y: PassivationState) =>
        (x, y) match {
          case (PassivationState.Disabled, PassivationState.Disabled)  => 0
          case (PassivationState.Disabled, PassivationState.After(_))  => -1
          case (PassivationState.Disabled, PassivationState.Unchanged) => -1
          case (PassivationState.After(before), PassivationState.After(after)) =>
            Order[FiniteDuration].compare(before, after)
          case (PassivationState.After(_), PassivationState.Disabled)   => 1
          case (PassivationState.After(_), PassivationState.Unchanged)  => -1
          case (PassivationState.Unchanged, PassivationState.Unchanged) => 0
          case (PassivationState.Unchanged, PassivationState.Disabled)  => 1
          case (PassivationState.Unchanged, PassivationState.After(_))  => 1
        }

    Order.by { ioaO =>
      unsafeRun(ioaO.run(None, algImpl[IO], PassivationState.Disabled))
        .fold(None, _ => None, fa => fa)
    }
  }

  implicit def cogenForEffectorT[F[_]: Monad, A](implicit
      cogenFA: Cogen[F[A]]
  ): Cogen[EffectorT[F, State, Alg, A]] =
    cogenFA.contramap(_.runA(Env(None, algImpl[F]), PassivationState.Disabled))

  implicit def arbitraryRun[F[_]: Monad, A](implicit
      A: Arbitrary[A]
  ): Arbitrary[(Env[State, Alg[F]], PassivationState) => F[(Unit, PassivationState, A)]] =
    Arbitrary(
      A.arbitrary.map(a =>
        (env: Env[State, Alg[F]], passivation: PassivationState) =>
          EffectorT.liftF(Applicative[F].pure(a)).run(env, passivation)
      )
    )

  implicit def execEffectorT[S](
      sbool: EffectorT[IO, S, Alg, Boolean]
  )(implicit ticker: Ticker): Prop =
    Prop(
      unsafeRun(sbool.run(None, algImpl[IO], PassivationState.Disabled)).fold(
        false,
        _ => false,
        pO => pO.fold(false) { case (_, _, bool) => bool }
      )
    )

  // check if resolves
  Functor[EffectorT[ListWrapper, State, Alg, *]]
  Applicative[EffectorT[ListWrapper, State, Alg, *]]
  Monad[EffectorT[ListWrapper, State, Alg, *]]
  MonadCancel[EffectorT[IO, State, Alg, *]]
  GenSpawn[EffectorT[IO, State, Alg, *]]
  GenConcurrent[EffectorT[IO, State, Alg, *]]
  Clock[EffectorT[IO, State, Alg, *]]
  GenTemporal[EffectorT[IO, State, Alg, *]]
  Sync[EffectorT[IO, State, Alg, *]]
  Async[EffectorT[IO, State, Alg, *]]

  checkAll(
    "EffectorT.MonadLaws",
    MonadTests[EffectorT[ListWrapper, State, Alg, *]].monad[Value, Value, Value]
  )

  checkAll(
    "EffectorT.AsyncLaws",
    AsyncTests[EffectorT[IO, State, Alg, *]].async[Value, Value, Value](10.millis)
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
