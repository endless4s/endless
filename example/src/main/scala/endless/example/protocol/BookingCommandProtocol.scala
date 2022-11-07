package endless.example.protocol

import endless.\/
import endless.circe.{CirceCommandProtocol, CirceDecoder}
import endless.core.protocol.{Decoder, IncomingCommand, OutgoingCommand}
import endless.example.algebra.BookingAlg
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown, CancelError}
import endless.example.data.Booking.BookingID
import endless.example.data.{Booking, LatLon}
import endless.example.protocol.BookingCommand._
import io.circe.generic.auto._

import java.time.Instant

//#example-client
class BookingCommandProtocol extends CirceCommandProtocol[BookingAlg] {
  override def client: BookingAlg[OutgoingCommand[*]] =
    new BookingAlg[OutgoingCommand[*]] {
      def place(
          bookingID: BookingID,
          time: Instant,
          passengerCount: Int,
          origin: LatLon,
          destination: LatLon
      ): OutgoingCommand[BookingAlreadyExists \/ Unit] = ???

      // ...
      // #example-client

      def get: OutgoingCommand[BookingUnknown.type \/ Booking] = ???

      def changeOrigin(
          newOrigin: LatLon
      ): OutgoingCommand[BookingUnknown.type \/ Unit] = ???

      def changeDestination(
          newDestination: LatLon
      ): OutgoingCommand[BookingUnknown.type \/ Unit] = ???

      def changeOriginAndDestination(
          newOrigin: LatLon,
          newDestination: LatLon
      ): OutgoingCommand[BookingUnknown.type \/ Unit] = ???

      override def cancel: OutgoingCommand[CancelError \/ Unit] = ???

      override def notifyCapacity(
          isAvailable: Boolean
      ): OutgoingCommand[BookingAlg.BookingUnknown.type \/ Unit] = ???
    }

//#example-server
  override def server[F[_]]: Decoder[IncomingCommand[F, BookingAlg]] = ???
}
