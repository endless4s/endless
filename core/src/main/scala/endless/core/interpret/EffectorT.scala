package endless.core.interpret

import cats.data.ReaderWriterStateT
import cats.effect.kernel._
import cats.syntax.all._
import cats.tagless.FunctorK
import cats.tagless.syntax.functorK._
import cats.{Applicative, Monad, ~>}
import endless.core.entity.Effector

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object EffectorT extends LoggerLiftingHelper {

  /** `EffectorT[F, S, A]` is a type alias for `ReaderWriterStateT` monad transformer from cats. It
    * uses `Reader` to allow access to ready-only entity state and algebra and `State` to update the
    * passivation activation schedule (`Writer` is unused). Instances are provided for cats-effect
    * kernel typeclasses, so that effector algebras can make use of e.g. `Async`.
    *
    * ``Implementation note: cats-effect itself purposefully omits instances of Concurrent and Async
    * for RWST, because the general semantics of State are understood as function composition, which
    * is inherently sequential and thus expects determinism. However, in the specific context of
    * EffectorT, where the state in RWST only represents PassivationState, we consider the
    * possibility of non-determinism acceptable. Indeed, its semantics are that the last emitted
    * passivation setting is the one applied to the entity (we don't provide a get). Therefore, even
    * if convoluted behavior would lead to two fibers racing to define the passivation state, the
    * end-result would be the same as e.g. encoding it with WriterT (which supports Concurrent)``
    * @tparam F
    *   context
    * @tparam S
    *   entity state
    * @tparam A
    *   value
    */
  type EffectorT[F[_], S, Alg[_[_]], A] =
    ReaderWriterStateT[F, Env[S, Alg[F]], Unit, PassivationState, A]

  def unit[F[_]: Applicative, S, Alg[_[_]]]: EffectorT[F, S, Alg, Unit] =
    liftF(Applicative[F].unit)

  def stateReader[F[_]: Applicative, S, Alg[_[_]]]: EffectorT[F, S, Alg, Option[S]] =
    ReaderWriterStateT.ask[F, Env[S, Alg[F]], Unit, PassivationState].map(_.state)

  def entityAlgReader[F[_]: Applicative, S, Alg[_[_]]]: EffectorT[F, S, Alg, Alg[F]] =
    ReaderWriterStateT.ask[F, Env[S, Alg[F]], Unit, PassivationState].map(_.entity)

  def passivationEnabler[F[_]: Applicative, S, Alg[_[_]]](
      after: FiniteDuration
  ): EffectorT[F, S, Alg, Unit] =
    ReaderWriterStateT.set(PassivationState.After(after))

  def passivationDisabler[F[_]: Applicative, S, Alg[_[_]]]: EffectorT[F, S, Alg, Unit] =
    ReaderWriterStateT.set(PassivationState.Disabled)

  implicit class EffectorTRunHelpers[F[_]: Monad, S, Alg[_[_]], A](
      val effectorT: EffectorT[F, S, Alg, A]
  ) {
    def runA(entityState: Option[S], entity: Alg[F]): F[A] =
      effectorT.runA(Env(entityState, entity), PassivationState.Unchanged)
    def runS(entityState: Option[S], entity: Alg[F]): F[PassivationState] =
      effectorT.runS(Env(entityState, entity), PassivationState.Unchanged)
    def run(
        entityState: Option[S],
        entity: Alg[F],
        passivationState: PassivationState
    ): F[(Unit, PassivationState, A)] =
      effectorT.run(Env(entityState, entity), passivationState)
  }

  final case class Env[S, Alg](state: Option[S], entity: Alg)

  sealed trait PassivationState
  object PassivationState {
    final case class After(duration: FiniteDuration) extends PassivationState
    object Disabled extends PassivationState
    object Unchanged extends PassivationState
  }

  implicit def instance[F[_]: Applicative, S, Alg[_[_]]: FunctorK]
      : Effector[EffectorT[F, S, Alg, *], S, Alg] =
    new Effector[EffectorT[F, S, Alg, *], S, Alg] {
      def read: EffectorT[F, S, Alg, Option[S]] = stateReader[F, S, Alg]
      def enablePassivation(after: FiniteDuration): EffectorT[F, S, Alg, Unit] = passivationEnabler(
        after
      )
      def disablePassivation: EffectorT[F, S, Alg, Unit] = passivationDisabler
      def self: EffectorT[F, S, Alg, Alg[EffectorT[F, S, Alg, *]]] =
        entityAlgReader[F, S, Alg].map(_.mapK(liftK))
    }

  def liftF[F[_]: Applicative, S, Alg[_[_]], A](fa: F[A]): EffectorT[F, S, Alg, A] =
    ReaderWriterStateT.liftF(fa)

  implicit def liftK[F[_]: Applicative, S, Alg[_[_]]]: F ~> EffectorT[F, S, Alg, *] =
    ReaderWriterStateT.liftK

  implicit def asyncForEffectorT[F[_], S, Alg[_[_]]](implicit
      F0: Async[F],
      K0: FunctorK[Alg]
  ): Async[EffectorT[F, S, Alg, *]] = new EffectorTAsync[F, S, Alg] {
    implicit protected def F: Async[F] = F0
    implicit protected def FunctorKAlg: FunctorK[Alg] = K0
  }

  private[interpret] trait EffectorTMonadCancel[F[_], S, Alg[_[_]], E]
      extends MonadCancel[EffectorT[F, S, Alg, *], E] {
    private val monadCancel =
      MonadCancel.monadCancelForReaderWriterStateT[F, Env[S, Alg[F]], Unit, PassivationState, E]

    implicit protected def F: MonadCancel[F, E]

    def forceR[A, B](fa: EffectorT[F, S, Alg, A])(
        fb: EffectorT[F, S, Alg, B]
    ): EffectorT[F, S, Alg, B] = monadCancel.forceR(fa)(fb)

    def uncancelable[A](
        body: Poll[EffectorT[F, S, Alg, *]] => EffectorT[F, S, Alg, A]
    ): EffectorT[F, S, Alg, A] = monadCancel.uncancelable(body)

    def canceled: EffectorT[F, S, Alg, Unit] = monadCancel.canceled

    def onCancel[A](
        fa: EffectorT[F, S, Alg, A],
        fin: EffectorT[F, S, Alg, Unit]
    ): EffectorT[F, S, Alg, A] = monadCancel.onCancel(fa, fin)

    def flatMap[A, B](fa: EffectorT[F, S, Alg, A])(
        f: A => EffectorT[F, S, Alg, B]
    ): EffectorT[F, S, Alg, B] = monadCancel.flatMap(fa)(f)

    def tailRecM[A, B](a: A)(f: A => EffectorT[F, S, Alg, Either[A, B]]): EffectorT[F, S, Alg, B] =
      monadCancel.tailRecM(a)(f)

    def raiseError[A](e: E): EffectorT[F, S, Alg, A] = monadCancel.raiseError(e)

    def handleErrorWith[A](fa: EffectorT[F, S, Alg, A])(
        f: E => EffectorT[F, S, Alg, A]
    ): EffectorT[F, S, Alg, A] = monadCancel.handleErrorWith(fa)(f)

    def pure[A](x: A): EffectorT[F, S, Alg, A] = monadCancel.pure(x)
  }

  private[interpret] trait EffectorTGenSpawn[F[_], S, Alg[_[_]], E]
      extends GenSpawn[EffectorT[F, S, Alg, *], E]
      with EffectorTMonadCancel[F, S, Alg, E] {
    implicit protected def F: GenSpawn[F, E]
    def start[A](
        fa: EffectorT[F, S, Alg, A]
    ): EffectorT[F, S, Alg, Fiber[EffectorT[F, S, Alg, *], E, A]] =
      ReaderWriterStateT { case (env, passivation) =>
        F
          .start(fa.run(env, passivation))
          .map(fiber => ((), passivation, liftFiber[A](fiber)))
      }

    private def liftOutcome[A](
        oc: Outcome[F, E, (Unit, PassivationState, A)]
    ): Outcome[EffectorT[F, S, Alg, *], E, A] = {
      oc match {
        case Outcome.Succeeded(fa) =>
          val fs = fa.map { case (_, passivation, _) => passivation }
          val faa = fa.map { case (_, _, a) => a }
          Outcome.Succeeded(
            ReaderWriterStateT
              .setF[F, Env[S, Alg[F]], Unit, PassivationState](fs)
              .flatMap(_ => ReaderWriterStateT.liftF(faa))
          )
        case Outcome.Errored(e) => Outcome.Errored(e)
        case Outcome.Canceled() => Outcome.Canceled()
      }
    }

    private def liftFiber[A](
        fib: Fiber[F, E, (Unit, PassivationState, A)]
    ): Fiber[EffectorT[F, S, Alg, *], E, A] =
      new Fiber[EffectorT[F, S, Alg, *], E, A] {
        def cancel: EffectorT[F, S, Alg, Unit] = liftF(fib.cancel)
        def join: EffectorT[F, S, Alg, Outcome[EffectorT[F, S, Alg, *], E, A]] =
          liftF(fib.join.map(liftOutcome))
      }

    def never[A]: EffectorT[F, S, Alg, A] = liftF(F.never)

    def cede: EffectorT[F, S, Alg, Unit] = liftF(F.cede)

    def racePair[A, B](
        fa: EffectorT[F, S, Alg, A],
        fb: EffectorT[F, S, Alg, B]
    ): EffectorT[
      F,
      S,
      Alg,
      Either[
        (Outcome[EffectorT[F, S, Alg, *], E, A], Fiber[EffectorT[F, S, Alg, *], E, B]),
        (Fiber[EffectorT[F, S, Alg, *], E, A], Outcome[EffectorT[F, S, Alg, *], E, B])
      ]
    ] = ReaderWriterStateT { case (env, passivation) =>
      F.uncancelable(poll =>
        poll(
          F
            .racePair(fa.run(env, passivation), fb.run(env, passivation))
            .map {
              case Left((oc, fib))  => Left((liftOutcome(oc), liftFiber(fib)))
              case Right((fib, oc)) => Right((liftFiber(fib), liftOutcome(oc)))
            }
            .map(((), passivation, _))
        )
      )
    }

    def unique: EffectorT[F, S, Alg, Unique.Token] = liftF(F.unique)
  }

  private[interpret] trait EffectorTGenConcurrent[F[_], S, Alg[_[_]], E]
      extends GenConcurrent[EffectorT[F, S, Alg, *], E]
      with EffectorTGenSpawn[F, S, Alg, E] {
    implicit protected def F: GenConcurrent[F, E]
    def ref[A](a: A): EffectorT[F, S, Alg, Ref[EffectorT[F, S, Alg, *], A]] =
      liftF(F.map(F.ref(a))(_.mapK(liftK)))
    def deferred[A]: EffectorT[F, S, Alg, Deferred[EffectorT[F, S, Alg, *], A]] =
      liftF(F.map(F.deferred[A])(_.mapK(liftK)))

    override def racePair[A, B](
        fa: EffectorT[F, S, Alg, A],
        fb: EffectorT[F, S, Alg, B]
    ): EffectorT[
      F,
      S,
      Alg,
      Either[
        (Outcome[EffectorT[F, S, Alg, *], E, A], Fiber[EffectorT[F, S, Alg, *], E, B]),
        (Fiber[EffectorT[F, S, Alg, *], E, A], Outcome[EffectorT[F, S, Alg, *], E, B])
      ]
    ] =
      super.racePair(fa, fb)
  }

  private[interpret] trait EffectorTClock[F[_], S, Alg[_[_]]]
      extends Clock[EffectorT[F, S, Alg, *]] {
    private val clock = Clock.clockForReaderWriterStateT[F, Env[S, Alg[F]], Unit, PassivationState]
    implicit protected def C: Clock[F]
    implicit protected def F: Monad[F]
    def applicative: Applicative[EffectorT[F, S, Alg, *]]
    def monotonic: EffectorT[F, S, Alg, FiniteDuration] = clock.monotonic
    def realTime: EffectorT[F, S, Alg, FiniteDuration] = clock.realTime
  }

  private[interpret] trait EffectorTGenTemporal[F[_], S, Alg[_[_]], E]
      extends GenTemporal[EffectorT[F, S, Alg, *], E]
      with EffectorTGenConcurrent[F, S, Alg, E]
      with EffectorTClock[F, S, Alg] {
    implicit protected def F: GenTemporal[F, E]
    protected def C: Clock[F] = F

    def sleep(time: FiniteDuration): EffectorT[F, S, Alg, Unit] = liftF(F.sleep(time))
  }

  private[interpret] trait EffectorTSync[F[_], S, Alg[_[_]]]
      extends Sync[EffectorT[F, S, Alg, *]]
      with EffectorTMonadCancel[F, S, Alg, Throwable]
      with EffectorTClock[F, S, Alg] {
    implicit protected def F: Sync[F]
    private val sync = Sync.syncForReaderWriterStateT[F, Env[S, Alg[F]], Unit, PassivationState]
    def suspend[A](hint: Sync.Type)(thunk: => A): EffectorT[F, S, Alg, A] =
      sync.suspend(hint)(thunk)
  }

  private[interpret] trait EffectorTAsync[F[_], S, Alg[_[_]]]
      extends Async[EffectorT[F, S, Alg, *]]
      with EffectorTSync[F, S, Alg]
      with EffectorTGenTemporal[F, S, Alg, Throwable] {
    implicit protected def F: Async[F]
    implicit protected def FunctorKAlg: FunctorK[Alg]

    def evalOn[A](fa: EffectorT[F, S, Alg, A], ec: ExecutionContext): EffectorT[F, S, Alg, A] =
      ReaderWriterStateT { case (env, passivation) => F.evalOn(fa.run(env, passivation), ec) }

    def executionContext: EffectorT[F, S, Alg, ExecutionContext] =
      liftF(F.executionContext)

    override def never[A]: EffectorT[F, S, Alg, A] = super.never

    override def unique: EffectorT[F, S, Alg, Unique.Token] = super.unique

    def cont[K, R](body: Cont[EffectorT[F, S, Alg, *], K, R]): EffectorT[F, S, Alg, R] =
      ReaderWriterStateT { case (env, passivation) =>
        F.cont(new Cont[F, K, (Unit, PassivationState, R)] {
          def apply[G[_]](implicit
              G: MonadCancel[G, Throwable]
          ): (Either[Throwable, K] => Unit, G[K], F ~> G) => G[(Unit, PassivationState, R)] =
            (cb, ga, nat) => {
              val natT: EffectorT[F, S, Alg, *] ~> EffectorT[G, S, Alg, *] =
                new ~>[EffectorT[F, S, Alg, *], EffectorT[G, S, Alg, *]] {
                  def apply[A](fa: EffectorT[F, S, Alg, A]): EffectorT[G, S, Alg, A] =
                    ReaderWriterStateT { case (_, _) =>
                      nat(fa.run(env, passivation))
                    }
                }
              body[EffectorT[G, S, Alg, *]]
                .apply(cb, liftF(ga), natT)
                .run(Env(env.state, FunctorK[Alg].mapK(env.entity)(nat)), passivation)
            }
        })
      }

  }
}
