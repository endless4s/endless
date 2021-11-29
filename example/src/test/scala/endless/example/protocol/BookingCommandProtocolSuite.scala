package endless.example.protocol

import cats.Id
import cats.syntax.functor._
import endless.\/
import endless.example.algebra.BookingAlg
import endless.example.algebra.BookingAlg.CancelError
import endless.example.data.Booking
import endless.example.logic.Generators
import org.scalacheck.Prop.forAll

import java.time.Instant

//#example
class BookingCommandProtocolSuite extends munit.ScalaCheckSuite with Generators {
  val protocol = new BookingCommandProtocol

  test("place booking") {
    forAll { (booking: Booking, reply: BookingAlg.BookingAlreadyExists \/ Unit) =>
      val outgoingCommand = protocol.client.place(
        booking.id,
        booking.time,
        booking.passengerCount,
        booking.origin,
        booking.destination
      )
      val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
      val encodedReply = incomingCommand
        .runWith(new TestBookingAlg {
          override def place(
              bookingID: Booking.BookingID,
              time: Instant,
              passengerCount: Int,
              origin: Booking.LatLon,
              destination: Booking.LatLon
          ): Id[BookingAlg.BookingAlreadyExists \/ Unit] = reply
        })
        .map(incomingCommand.replyEncoder.encode(_))
      assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }
//#example

  test("get booking") {
    forAll { (reply: BookingAlg.BookingUnknown.type \/ Booking) =>
      val outgoingCommand = protocol.client.get
      val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
      val encodedReply = incomingCommand
        .runWith(new TestBookingAlg {
          override def get: Id[BookingAlg.BookingUnknown.type \/ Booking] = reply
        })
        .map(incomingCommand.replyEncoder.encode(_))
      assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }

  test("change origin") {
    forAll { (newOrigin: Booking.LatLon, reply: BookingAlg.BookingUnknown.type \/ Unit) =>
      val outgoingCommand = protocol.client.changeOrigin(newOrigin)
      val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
      val encodedReply = incomingCommand
        .runWith(new TestBookingAlg {
          override def changeOrigin(
              origin: Booking.LatLon
          ): Id[BookingAlg.BookingUnknown.type \/ Unit] = reply
        })
        .map(incomingCommand.replyEncoder.encode(_))
      assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }

  test("change destination") {
    forAll { (newDestination: Booking.LatLon, reply: BookingAlg.BookingUnknown.type \/ Unit) =>
      val outgoingCommand = protocol.client.changeDestination(newDestination)
      val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
      val encodedReply = incomingCommand
        .runWith(new TestBookingAlg {
          override def changeDestination(
              destination: Booking.LatLon
          ): Id[BookingAlg.BookingUnknown.type \/ Unit] = reply
        })
        .map(incomingCommand.replyEncoder.encode(_))
      assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }

  test("change origin and destination") {
    forAll {
      (
          newOrigin: Booking.LatLon,
          newDestination: Booking.LatLon,
          reply: BookingAlg.BookingUnknown.type \/ Unit
      ) =>
        val outgoingCommand = protocol.client.changeOriginAndDestination(newOrigin, newDestination)
        val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
        val encodedReply = incomingCommand
          .runWith(new TestBookingAlg {
            override def changeOriginAndDestination(
                origin: Booking.LatLon,
                destination: Booking.LatLon
            ): Id[BookingAlg.BookingUnknown.type \/ Unit] = reply
          })
          .map(incomingCommand.replyEncoder.encode(_))
        assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }

  test("cancel") {
    forAll { (reply: CancelError \/ Unit) =>
      val outgoingCommand = protocol.client.cancel
      val incomingCommand = protocol.server[Id].decode(outgoingCommand.payload)
      val encodedReply = incomingCommand
        .runWith(new TestBookingAlg {
          override def cancel: Id[CancelError \/ Unit] = reply
        })
        .map(incomingCommand.replyEncoder.encode(_))
      assertEquals(outgoingCommand.replyDecoder.decode(encodedReply), reply)
    }
  }

  trait TestBookingAlg extends BookingAlg[Id] {
    def place(
        bookingID: Booking.BookingID,
        time: Instant,
        passengerCount: Int,
        origin: Booking.LatLon,
        destination: Booking.LatLon
    ): Id[BookingAlg.BookingAlreadyExists \/ Unit] = throw new RuntimeException(
      "not supposed to be called"
    )
    def get: Id[BookingAlg.BookingUnknown.type \/ Booking] = throw new RuntimeException(
      "not supposed to be called"
    )
    def changeOrigin(newOrigin: Booking.LatLon): Id[BookingAlg.BookingUnknown.type \/ Unit] =
      throw new RuntimeException("not supposed to be called")
    def changeDestination(
        newDestination: Booking.LatLon
    ): Id[BookingAlg.BookingUnknown.type \/ Unit] = throw new RuntimeException(
      "not supposed to be called"
    )
    def changeOriginAndDestination(
        newOrigin: Booking.LatLon,
        newDestination: Booking.LatLon
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
