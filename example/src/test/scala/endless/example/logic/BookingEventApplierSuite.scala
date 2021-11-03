package endless.example.logic

import endless.example.data.Booking
import endless.example.data.Booking.LatLon
import endless.example.data.BookingEvent.{BookingPlaced, DestinationChanged, OriginChanged}
import org.scalacheck.Prop._

//#example
class BookingEventApplierSuite extends munit.ScalaCheckSuite with Generators {
  property("booking placed when unknown") {
    forAll { booking: Booking =>
      val fold = new BookingEventApplier()(
        None,
        BookingPlaced(booking.id, booking.origin, booking.destination, booking.passengerCount)
      )
      assertEquals(fold, Right(Some(booking)))
    }
  }

  property("booking placed when known") {
    forAll { booking: Booking =>
      val fold = new BookingEventApplier()(
        Some(booking),
        BookingPlaced(booking.id, booking.origin, booking.destination, booking.passengerCount)
      )
      assert(fold.isLeft)
    }
  }

  property("origin changed when known") {
    forAll { (booking: Booking, newOrigin: LatLon) =>
      val fold = new BookingEventApplier()(Some(booking), OriginChanged(newOrigin))
      assertEquals(fold.toOption.flatMap(_.map(_.origin)), Option(newOrigin))
    }
  }

  property("origin changed when unknown") {
    forAll { newOrigin: LatLon =>
      val fold = new BookingEventApplier()(None, OriginChanged(newOrigin))
      assert(fold.isLeft)
    }
  }

  property("destination changed when unknown") {
    forAll { newDestination: LatLon =>
      val fold = new BookingEventApplier()(None, DestinationChanged(newDestination))
      assert(fold.isLeft)
    }
  }
}
//#example
