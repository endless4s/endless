package endless.example.logic
import cats.data.Chain
import cats.effect.IO
import endless.core.interpret.EntityT
import endless.core.interpret.EntityT._
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown, BookingWasRejected}
import endless.example.data.Booking.LatLon
import endless.example.data.BookingEvent._
import endless.example.data.{Booking, BookingEvent}
import org.scalacheck.effect.PropF._
import org.typelevel.log4cats.testing.TestingLogger

//#example
class BookingEntitySuite
    extends munit.CatsEffectSuite
    with munit.ScalaCheckEffectSuite
    with Generators {
  implicit private val logger: TestingLogger[IO] = TestingLogger.impl[IO]()
  private val bookingAlg = BookingEntity(EntityT.instance[IO, Booking, BookingEvent])
  private implicit val eventApplier: BookingEventApplier = new BookingEventApplier

  test("place booking") {
    forAllF { booking: Booking =>
      bookingAlg
        .place(
          booking.id,
          booking.time,
          booking.passengerCount,
          booking.origin,
          booking.destination
        )
        .run(None)
        .map {
          case Right((events, _)) =>
            assertEquals(
              events,
              Chain(
                BookingPlaced(
                  booking.id,
                  booking.time,
                  booking.origin,
                  booking.destination,
                  booking.passengerCount
                )
              )
            )
          case Left(error) => fail(error)
        }
        .flatMap(_ => assertIOBoolean(logger.logged.map(_.nonEmpty)))
    }
  }

  test("change origin and destination") {
    forAllF { (booking: Booking, newOrigin: LatLon, newDestination: LatLon) =>
      bookingAlg
        .changeOriginAndDestination(newOrigin, newDestination)
        .run(Some(booking))
        .map {
          case Right((events, _)) =>
            assertEquals(
              events,
              Chain[BookingEvent](OriginChanged(newOrigin), DestinationChanged(newDestination))
            )
          case _ => fail("unexpected")
        }
    }
  }
//#example

  test("place booking when it already exists") {
    forAllF { booking: Booking =>
      bookingAlg
        .place(
          booking.id,
          booking.time,
          booking.passengerCount,
          booking.origin,
          booking.destination
        )
        .run(Some(booking))
        .map {
          case Right((_, Left(alreadyExists))) =>
            assertEquals(alreadyExists, BookingAlreadyExists(booking.id))
          case _ => fail("unexpected")
        }
    }
  }

  test("get booking") {
    forAllF { booking: Booking =>
      bookingAlg.get
        .run(Some(booking))
        .map {
          case Right((events, Right(state))) =>
            assert(events.isEmpty)
            assertEquals(state, booking)
          case _ => fail("unexpected")
        }
    }
  }

  test("get booking when unknown") {
    bookingAlg.get
      .run(None)
      .map {
        case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
        case _                         => fail("unexpected")
      }
  }

  test("change origin") {
    forAllF { (booking: Booking, newOrigin: LatLon) =>
      bookingAlg
        .changeOrigin(newOrigin)
        .run(Some(booking))
        .map {
          case Right((events, _)) =>
            assertEquals(events, Chain(OriginChanged(newOrigin)))
          case _ => fail("unexpected")
        }
    }
  }

  test("change origin when unknown") {
    forAllF { newOrigin: LatLon =>
      bookingAlg
        .changeOrigin(newOrigin)
        .run(None)
        .map {
          case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
          case _                         => fail("unexpected")
        }
    }
  }

  test("change destination") {
    forAllF { (booking: Booking, newDestination: LatLon) =>
      bookingAlg
        .changeDestination(newDestination)
        .run(Some(booking))
        .map {
          case Right((events, _)) =>
            assertEquals(events, Chain(DestinationChanged(newDestination)))
          case _ => fail("unexpected")
        }
    }
  }

  test("change destination when unknown") {
    forAllF { newDestination: LatLon =>
      bookingAlg
        .changeDestination(newDestination)
        .run(None)
        .map {
          case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
          case _                         => fail("unexpected")
        }
    }
  }

  test("change origin and destination when unknown") {
    forAllF { (newOrigin: LatLon, newDestination: LatLon) =>
      bookingAlg
        .changeOriginAndDestination(newOrigin, newDestination)
        .run(None)
        .map {
          case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
          case _                         => fail("unexpected")
        }
    }
  }

  test("cancel booking") {
    forAllF { booking: Booking =>
      bookingAlg.cancel.run(Some(booking)).map {
        case Right((events, _)) =>
          assertEquals(events, Chain(BookingCancelled))
        case _ => fail("unexpected")
      }
    }
  }

  test("cancel booking when rejected") {
    forAllF { booking: Booking =>
      bookingAlg.cancel.run(Some(booking.copy(status = Booking.Status.Rejected))).map {
        case Right((_, Left(bookingWasRejected))) =>
          assertEquals(bookingWasRejected, BookingWasRejected(booking.id))
        case _ => fail("unexpected")
      }
    }
  }

  test("cancel booking when unknown") {
    bookingAlg.cancel.run(None).map {
      case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
      case _                         => fail("unexpected")
    }
  }

  test("notify capacity available") {
    forAllF { booking: Booking =>
      bookingAlg.notifyCapacity(true).run(Some(booking)).map {
        case Right((events, _)) =>
          assertEquals(events, Chain(BookingAccepted))
        case _ => fail("unexpected")
      }
    }
  }

  test("notify capacity available when unknown") {
    bookingAlg.notifyCapacity(true).run(None).map {
      case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
      case _                         => fail("unexpected")
    }
  }

  test("notify capacity unavailable") {
    forAllF { booking: Booking =>
      bookingAlg.notifyCapacity(false).run(Some(booking)).map {
        case Right((events, _)) =>
          assertEquals(events, Chain(BookingRejected))
        case _ => fail("unexpected")
      }
    }
  }
}
