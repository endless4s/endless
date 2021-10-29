package endless.example.logic
import cats.data.Chain
import cats.effect.IO
import endless.core.data.EventsFolder
import endless.core.interpret.EntityT
import endless.core.interpret.EntityT._
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown}
import endless.example.data.Booking.LatLon
import endless.example.data.BookingEvent.{BookingPlaced, DestinationChanged, OriginChanged}
import endless.example.data.{Booking, BookingEvent}
import org.scalacheck.effect.PropF._
import org.typelevel.log4cats.testing.TestingLogger

class BookingEntitySuite
    extends munit.CatsEffectSuite
    with munit.ScalaCheckEffectSuite
    with Generators {
  implicit private val logger: TestingLogger[IO] = TestingLogger.impl[IO]()
  private val bookingAlg = BookingEntity(EntityT.instance[IO, Booking, BookingEvent])

  test("place booking") {
    forAllF { booking: Booking =>
      bookingAlg
        .place(booking.id, booking.passengerCount, booking.origin, booking.destination)
        .run(EventsFolder(state = None, new BookingEventApplier))
        .map {
          case Right((events, _)) =>
            assertEquals(
              events,
              Chain(
                BookingPlaced(
                  booking.id,
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

  test("place booking when it already exists") {
    forAllF { booking: Booking =>
      bookingAlg
        .place(booking.id, booking.passengerCount, booking.origin, booking.destination)
        .run(
          EventsFolder(
            state = Some(
              Booking(booking.id, booking.origin, booking.destination, booking.passengerCount)
            ),
            new BookingEventApplier
          )
        )
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
        .run(EventsFolder(state = Some(booking), new BookingEventApplier))
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
      .run(EventsFolder(state = None, new BookingEventApplier))
      .map {
        case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
        case _                         => fail("unexpected")
      }
  }

  test("change origin") {
    forAllF { (booking: Booking, newOrigin: LatLon) =>
      bookingAlg
        .changeOrigin(newOrigin)
        .run(EventsFolder(Some(booking), new BookingEventApplier))
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
        .run(EventsFolder(None, new BookingEventApplier))
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
        .run(EventsFolder(Some(booking), new BookingEventApplier))
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
        .run(EventsFolder(None, new BookingEventApplier))
        .map {
          case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
          case _                         => fail("unexpected")
        }
    }
  }

  test("change origin and destination") {
    forAllF { (booking: Booking, newOrigin: LatLon, newDestination: LatLon) =>
      bookingAlg
        .changeOriginAndDestination(newOrigin, newDestination)
        .run(EventsFolder(Some(booking), new BookingEventApplier))
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

  test("change origin and destination when unknown") {
    forAllF { (newOrigin: LatLon, newDestination: LatLon) =>
      bookingAlg
        .changeOriginAndDestination(newOrigin, newDestination)
        .run(EventsFolder(None, new BookingEventApplier))
        .map {
          case Right((_, Left(unknown))) => assertEquals(unknown, BookingUnknown)
          case _                         => fail("unexpected")
        }
    }
  }

}
