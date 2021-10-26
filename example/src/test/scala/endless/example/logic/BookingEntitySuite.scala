package endless.example.logic
import cats.data.Chain
import cats.effect.IO
import endless.core.data.EventsFolder
import endless.core.interpret.EntityT
import endless.core.interpret.EntityT._
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown}
import endless.example.data.Booking.{BookingID, LatLon}
import endless.example.data.BookingEvent.{BookingPlaced, DestinationChanged, OriginChanged}
import endless.example.data.{Booking, BookingEvent}
import endless.example.logic.BookingEntitySuite._
import org.scalacheck.effect.PropF
import org.scalacheck.{Arbitrary, Gen}
import org.typelevel.log4cats.testing.TestingLogger

class BookingEntitySuite extends munit.CatsEffectSuite with munit.ScalaCheckEffectSuite {
  implicit private val logger = TestingLogger.impl[IO]()
  private val bookingAlg = BookingEntity(EntityT.instance[IO, Booking, BookingEvent])

  test("place booking") {
    PropF.forAllF { booking: Booking =>
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
    PropF.forAllF { booking: Booking =>
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
    PropF.forAllF { booking: Booking =>
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
    PropF.forAllF { (booking: Booking, newOrigin: LatLon) =>
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
    PropF.forAllF { newOrigin: LatLon =>
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
    PropF.forAllF { (booking: Booking, newDestination: LatLon) =>
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
    PropF.forAllF { newDestination: LatLon =>
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
    PropF.forAllF { (booking: Booking, newOrigin: LatLon, newDestination: LatLon) =>
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
    PropF.forAllF { (newOrigin: LatLon, newDestination: LatLon) =>
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

object BookingEntitySuite {
  implicit val latLonGen: Gen[LatLon] = for {
    latitude <- Gen.double
    longitude <- Gen.double
  } yield LatLon(latitude, longitude)
  implicit val bookingIDGen: Gen[BookingID] = Gen.uuid.map(BookingID(_))
  implicit val bookingGen: Gen[Booking] = for {
    id <- bookingIDGen
    origin <- latLonGen
    destination <- latLonGen
    passengerCount <- Gen.posNum[Int]
  } yield Booking(id, origin, destination, passengerCount)
  implicit val arbBooking: Arbitrary[Booking] = Arbitrary(bookingGen)
  implicit val arbLatLon: Arbitrary[LatLon] = Arbitrary(latLonGen)
}
