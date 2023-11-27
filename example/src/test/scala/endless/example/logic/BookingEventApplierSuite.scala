package endless.example.logic

import endless.example.data.BookingEvent.*
import endless.example.data.{Booking, LatLon}
import org.scalacheck.Prop.*

//#example
class BookingEventApplierSuite extends munit.ScalaCheckSuite with Generators {
  property("booking placed when unknown") {
    forAll { (booking: Booking) =>
      val fold = new BookingEventApplier()(
        None,
        BookingPlaced(
          booking.id,
          booking.time,
          booking.origin,
          booking.destination,
          booking.passengerCount
        )
      )
      assertEquals(fold, Right(Some(booking)))
    }
  }

  property("booking placed when known") {
    forAll { (booking: Booking) =>
      val fold = new BookingEventApplier()(
        Some(booking),
        BookingPlaced(
          booking.id,
          booking.time,
          booking.origin,
          booking.destination,
          booking.passengerCount
        )
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
    forAll { (newOrigin: LatLon) =>
      val fold = new BookingEventApplier()(None, OriginChanged(newOrigin))
      assert(fold.isLeft)
    }
  }

  property("destination changed when known") {
    forAll { (booking: Booking, newDestination: LatLon) =>
      val fold = new BookingEventApplier()(Some(booking), DestinationChanged(newDestination))
      assertEquals(fold.toOption.flatMap(_.map(_.destination)), Option(newDestination))
    }
  }

  property("destination changed when unknown") {
    forAll { (newDestination: LatLon) =>
      val fold = new BookingEventApplier()(None, DestinationChanged(newDestination))
      assert(fold.isLeft)
    }
  }

  property("booking accepted when known") {
    forAll { (booking: Booking) =>
      val fold = new BookingEventApplier()(Some(booking), BookingAccepted)
      assertEquals(fold.toOption.flatMap(_.map(_.status)), Option(Booking.Status.Accepted))
    }
  }

  test("booking accepted when unknown") {
    val fold = new BookingEventApplier()(None, BookingAccepted)
    assert(fold.isLeft)
  }

  property("booking rejected when known") {
    forAll { (booking: Booking) =>
      val fold = new BookingEventApplier()(Some(booking), BookingRejected)
      assertEquals(fold.toOption.flatMap(_.map(_.status)), Option(Booking.Status.Rejected))
    }
  }

  test("booking rejected when unknown") {
    val fold = new BookingEventApplier()(None, BookingRejected)
    assert(fold.isLeft)
  }

  property("booking cancelled when known") {
    forAll { (booking: Booking) =>
      val fold = new BookingEventApplier()(Some(booking), BookingCancelled)
      assertEquals(fold.toOption.flatMap(_.map(_.status)), Option(Booking.Status.Cancelled))
    }
  }

  test("booking cancelled when unknown") {
    val fold = new BookingEventApplier()(None, BookingCancelled)
    assert(fold.isLeft)
  }
}
//#example
