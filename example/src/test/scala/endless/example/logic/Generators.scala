package endless.example.logic

import endless.example.algebra.BookingAlg.{
  BookingAlreadyExists,
  BookingUnknown,
  BookingWasRejected,
  CancelError
}
import endless.example.data.Booking.BookingID
import endless.example.data.{Booking, LatLon, Speed}
import org.scalacheck.{Arbitrary, Gen}

import java.time.Instant

trait Generators {
  implicit val latLonGen: Gen[LatLon] = for {
    latitude <- Gen.double
    longitude <- Gen.double
  } yield LatLon(latitude, longitude)
  implicit val speedGen: Gen[Speed] = Gen.double.map(Speed(_))
  implicit val instantGen: Gen[Instant] = Gen.long.map(Instant.ofEpochMilli)
  implicit val bookingIDGen: Gen[BookingID] = Gen.uuid.map(BookingID(_))
  implicit val bookingGen: Gen[Booking] = for {
    id <- bookingIDGen
    time <- instantGen
    origin <- latLonGen
    destination <- latLonGen
    passengerCount <- Gen.posNum[Int]
  } yield Booking(id, time, origin, destination, passengerCount)
  implicit val bookingAlreadyExists: Gen[BookingAlreadyExists] =
    Gen.uuid.map(uuid => BookingAlreadyExists(BookingID(uuid)))
  implicit val bookingUnknownGen: Gen[BookingUnknown.type] = Gen.const(BookingUnknown)
  implicit val cancelErrorGen: Gen[CancelError] =
    Gen.oneOf(bookingUnknownGen, bookingIDGen.map(BookingWasRejected.apply))

  implicit val arbBookingID: Arbitrary[BookingID] = Arbitrary(bookingIDGen)
  implicit val arbBooking: Arbitrary[Booking] = Arbitrary(bookingGen)
  implicit val arbLatLon: Arbitrary[LatLon] = Arbitrary(latLonGen)
  implicit val arbSpeed: Arbitrary[Speed] = Arbitrary(speedGen)
  implicit val arbBookingAlreadyExists: Arbitrary[BookingAlreadyExists] = Arbitrary(
    bookingAlreadyExists
  )
  implicit val arbBookingUnknown: Arbitrary[BookingUnknown.type] = Arbitrary(bookingUnknownGen)
  implicit val arbCancelError: Arbitrary[CancelError] = Arbitrary(cancelErrorGen)
}
