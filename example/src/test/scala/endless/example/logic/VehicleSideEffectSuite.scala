package endless.example.logic

import cats.effect.IO
import endless.core.entity.Effector
import endless.core.entity.SideEffect.Trigger
import endless.example.algebra.VehicleAlg
import endless.example.data.{LatLon, Speed, Vehicle}

import scala.concurrent.duration.*

class VehicleSideEffectSuite extends munit.CatsEffectSuite {

  test("when entity just recovered, increments recovery count") {
    for {
      recoveryCountRef <- IO.ref(0)
      effector <- Effector.apply[IO, Vehicle, VehicleAlg](
        new SelfEntity {
          override def incrementRecoveryCount: IO[Unit] = recoveryCountRef.update(_ + 1)
        },
        None
      )
      sideEffect = new VehicleSideEffect[IO]
      _ <- sideEffect.apply(Trigger.AfterRecovery, effector)
      _ <- assertIO(recoveryCountRef.get, 1)
    } yield ()
  }

  test("does not increment recovery count otherwise") {
    for {
      recoveryCountRef <- IO.ref(0)
      effector <- Effector.apply[IO, Vehicle, VehicleAlg](
        new SelfEntity {
          override def incrementRecoveryCount: IO[Unit] = recoveryCountRef.update(_ + 1)
        },
        None
      )
      sideEffect = new VehicleSideEffect[IO]
      _ <- sideEffect.apply(Trigger.AfterRead, effector)
      _ <- sideEffect.apply(Trigger.AfterPersistence, effector)
      _ <- assertIO(recoveryCountRef.get, 0)
    } yield ()
  }

  test("enables aggressive passivation") {
    for {
      effector <- Effector.apply[IO, Vehicle, VehicleAlg](
        new SelfEntity {
          override def incrementRecoveryCount: IO[Unit] = IO.unit
        },
        None
      )
      sideEffect = new VehicleSideEffect[IO]
      _ <- sideEffect.apply(Trigger.AfterRead, effector)
      _ <- assertIO(effector.passivationState, Effector.PassivationState.After(1.second))
    } yield ()
  }

  trait SelfEntity extends VehicleAlg[IO] {
    val raiseError = IO.raiseError(new RuntimeException("should not be called"))

    def setSpeed(speed: Speed): IO[Unit] = raiseError
    def setPosition(position: LatLon): IO[Unit] = raiseError
    def getSpeed: IO[Option[Speed]] = raiseError
    def getPosition: IO[Option[LatLon]] = raiseError
    def getRecoveryCount: IO[Int] = raiseError
    def incrementRecoveryCount: IO[Unit] = raiseError
  }
}
