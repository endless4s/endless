package endless.example.logic

import cats.effect.IO
import endless.core.interpret.DurableEntityT
import endless.core.interpret.DurableEntityT.*
import endless.example.data.{LatLon, Speed, Vehicle}
import org.scalacheck.effect.PropF.*
import org.typelevel.log4cats.testing.TestingLogger

class VehicleEntityBehaviorSuite
    extends munit.CatsEffectSuite
    with munit.ScalaCheckEffectSuite
    with Generators {
  implicit private val logger: TestingLogger[IO] = TestingLogger.impl[IO]()
  private val vehicleAlg = VehicleEntityBehavior(DurableEntityT.instance[IO, Vehicle])

  test("set position") {
    forAllF { (latLon: LatLon) =>
      vehicleAlg
        .setPosition(latLon)
        .run(State.None)
        .map {
          case (State.Updated(vehicle), ()) =>
            assertEquals(vehicle.position, Some(latLon))
          case _ => fail("incorrect vehicle state")
        }
        .flatMap(_ => assertIOBoolean(logger.logged.map(_.nonEmpty)))
    }
  }

  test("set speed") {
    forAllF { (speed: Speed) =>
      vehicleAlg
        .setSpeed(speed)
        .run(State.None)
        .map {
          case (State.Updated(vehicle), ()) =>
            assertEquals(vehicle.speed, Some(speed))
          case _ => fail("incorrect vehicle state")
        }
        .flatMap(_ => assertIOBoolean(logger.logged.map(_.nonEmpty)))
    }
  }

  test("get speed") {
    forAllF { (speed: Option[Speed], latLon: Option[LatLon]) =>
      vehicleAlg.getSpeed
        .runA(State.Existing(Vehicle(latLon, speed)))
        .map(result => assertEquals(result, speed))
    }
  }

  test("get speed when unknown") {
    assertIOBoolean(
      vehicleAlg.getSpeed
        .runA(State.None)
        .map(_.isEmpty)
    )
  }

  test("get position") {
    forAllF { (speed: Option[Speed], latLon: Option[LatLon]) =>
      vehicleAlg.getPosition
        .runA(State.Existing(Vehicle(latLon, speed)))
        .map(result => assertEquals(result, latLon))
    }
  }

  test("get position when unknown") {
    assertIOBoolean(
      vehicleAlg.getPosition
        .runA(State.None)
        .map(_.isEmpty)
    )
  }

  test("get recovery count") {
    forAllF { (speed: Option[Speed], latLon: Option[LatLon], recoveryCount: Int) =>
      vehicleAlg.getRecoveryCount
        .runA(State.Existing(Vehicle(latLon, speed, recoveryCount)))
        .map(result => assertEquals(result, recoveryCount))
    }
  }

  test("get recovery count when unknown") {
    assertIOBoolean(
      vehicleAlg.getRecoveryCount
        .runA(State.None)
        .map(_ == 0)
    )
  }

  test("increment recovery count") {
    forAllF { (speed: Option[Speed], latLon: Option[LatLon], recoveryCount: Int) =>
      vehicleAlg.incrementRecoveryCount
        .run(State.Existing(Vehicle(latLon, speed, recoveryCount)))
        .map {
          case (State.Updated(vehicle), ()) =>
            assertEquals(vehicle.recoveryCount, recoveryCount + 1)
          case _ => fail("incorrect vehicle state")
        }
    }
  }
}
