package endless.example.logic

import endless.example.data.Booking
import endless.example.data.Booking.{BookingID, LatLon}
import org.scalacheck.{Arbitrary, Gen}

trait Generators {
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
