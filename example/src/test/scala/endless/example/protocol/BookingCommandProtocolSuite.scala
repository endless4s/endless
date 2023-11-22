package endless.example.protocol

import cats.Id
import endless.\/
import endless.core.protocol.CommandSender
import endless.example.algebra.BookingAlg
import endless.example.algebra.BookingAlg.CancelError
import endless.example.data.Booking.BookingID
import endless.example.data.{Booking, LatLon}
import endless.example.logic.Generators
import org.scalacheck.Prop.forAll

import java.time.Instant

//#example
class BookingCommandProtocolSuite extends munit.ScalaCheckSuite with Generators {

  test("place booking") {
    forAll { (booking: Booking, reply: BookingAlg.BookingAlreadyExists \/ Unit) =>
      implicit val sender: CommandSender[Id, Booking.BookingID] = CommandSender.local(
        protocol,
        new TestBookingAlg {
          override def place(
              bookingID: Booking.BookingID,
              time: Instant,
              passengerCount: Int,
              origin: LatLon,
              destination: LatLon
          ): Id[BookingAlg.BookingAlreadyExists \/ Unit] = reply
        }
      )
      val actualReply = protocol
        .clientFor(booking.id)
        .place(
          booking.id,
          booking.time,
          booking.passengerCount,
          booking.origin,
          booking.destination
        )
      assertEquals(actualReply, reply)
    }
  }
//#example

  test("get booking") {
    forAll { (id: BookingID, reply: BookingAlg.BookingUnknown.type \/ Booking) =>
      implicit val sender: CommandSender[Id, Booking.BookingID] = CommandSender.local(
        protocol,
        new TestBookingAlg {
          override def get: Id[BookingAlg.BookingUnknown.type \/ Booking] = reply
        }
      )
      val actualReply = protocol.clientFor(id).get
      assertEquals(actualReply, reply)
    }
  }

  test("change origin") {
    forAll { (id: BookingID, newOrigin: LatLon, reply: BookingAlg.BookingUnknown.type \/ Unit) =>
      implicit val sender: CommandSender[Id, Booking.BookingID] = CommandSender.local(
        protocol,
        new TestBookingAlg {
          override def changeOrigin(newOrigin: LatLon): Id[BookingAlg.BookingUnknown.type \/ Unit] =
            reply
        }
      )
      val actualReply = protocol.clientFor(id).changeOrigin(newOrigin)
      assertEquals(actualReply, reply)
    }
  }

  test("change destination") {
    forAll {
      (id: BookingID, newDestination: LatLon, reply: BookingAlg.BookingUnknown.type \/ Unit) =>
        implicit val sender: CommandSender[Id, BookingID] = CommandSender.local(
          protocol,
          new TestBookingAlg {
            override def changeDestination(
                newDestination: LatLon
            ): Id[BookingAlg.BookingUnknown.type \/ Unit] = reply
          }
        )
        val actualReply = protocol.clientFor(id).changeDestination(newDestination)
        assertEquals(actualReply, reply)
    }
  }

  test("change origin and destination") {
    forAll {
      (
          bookingID: BookingID,
          newOrigin: LatLon,
          newDestination: LatLon,
          reply: BookingAlg.BookingUnknown.type \/ Unit
      ) =>
        implicit val sender: CommandSender[Id, BookingID] = CommandSender.local(
          protocol,
          new TestBookingAlg {
            override def changeOriginAndDestination(
                newOrigin: LatLon,
                newDestination: LatLon
            ): Id[BookingAlg.BookingUnknown.type \/ Unit] = reply
          }
        )
        val actualReply = protocol
          .clientFor(bookingID)
          .changeOriginAndDestination(newOrigin, newDestination)
        assertEquals(actualReply, reply)
    }
  }

  test("cancel") {
    forAll { (bookingID: BookingID, reply: CancelError \/ Unit) =>
      implicit val sender: CommandSender[Id, BookingID] = CommandSender.local(
        protocol,
        new TestBookingAlg {
          override def cancel: Id[CancelError \/ Unit] = reply
        }
      )
      val actualReply = protocol.clientFor(bookingID).cancel
      assertEquals(actualReply, reply)
    }
  }

  val protocol = new BookingCommandProtocol

  trait TestBookingAlg extends BookingAlg[Id] {
    def place(
        bookingID: Booking.BookingID,
        time: Instant,
        passengerCount: Int,
        origin: LatLon,
        destination: LatLon
    ): Id[BookingAlg.BookingAlreadyExists \/ Unit] = throw new RuntimeException(
      "not supposed to be called"
    )
    def get: Id[BookingAlg.BookingUnknown.type \/ Booking] = throw new RuntimeException(
      "not supposed to be called"
    )
    def changeOrigin(newOrigin: LatLon): Id[BookingAlg.BookingUnknown.type \/ Unit] =
      throw new RuntimeException("not supposed to be called")
    def changeDestination(
        newDestination: LatLon
    ): Id[BookingAlg.BookingUnknown.type \/ Unit] = throw new RuntimeException(
      "not supposed to be called"
    )
    def changeOriginAndDestination(
        newOrigin: LatLon,
        newDestination: LatLon
    ): Id[BookingAlg.BookingUnknown.type \/ Unit] = throw new RuntimeException(
      "not supposed to be called"
    )
    def cancel: Id[CancelError \/ Unit] = throw new RuntimeException(
      "not supposed to be called"
    )
    def notifyCapacity(isAvailable: Boolean): Id[BookingAlg.BookingUnknown.type \/ Unit] =
      throw new RuntimeException("not supposed to be called")
  }
}
