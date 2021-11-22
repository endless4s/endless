package endless.example.protocol

import endless.\/
import endless.circe.{CirceCommandProtocol, CirceDecoder}
import endless.core.typeclass.protocol.{Decoder, IncomingCommand, OutgoingCommand}
import endless.example.algebra.BookingAlg
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown, CancelError}
import endless.example.data.Booking
import endless.example.data.Booking.{BookingID, LatLon}
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
      ): OutgoingCommand[BookingAlreadyExists \/ Unit] =
        outgoingCommand[BookingCommand, BookingAlreadyExists \/ Unit](
          PlaceBooking(bookingID, time, passengerCount, origin, destination)
        )

      // ...
      // #example-client

      def get: OutgoingCommand[BookingUnknown.type \/ Booking] =
        outgoingCommand[BookingCommand, BookingUnknown.type \/ Booking](Get)

      def changeOrigin(
          newOrigin: LatLon
      ): OutgoingCommand[BookingUnknown.type \/ Unit] =
        outgoingCommand[BookingCommand, BookingUnknown.type \/ Unit](
          ChangeOrigin(newOrigin)
        )

      def changeDestination(
          newDestination: LatLon
      ): OutgoingCommand[BookingUnknown.type \/ Unit] =
        outgoingCommand[BookingCommand, BookingUnknown.type \/ Unit](
          ChangeDestination(newDestination)
        )

      def changeOriginAndDestination(
          newOrigin: LatLon,
          newDestination: LatLon
      ): OutgoingCommand[BookingUnknown.type \/ Unit] =
        outgoingCommand[BookingCommand, BookingUnknown.type \/ Unit](
          ChangeOriginAndDestination(newOrigin, newDestination)
        )

      override def cancel: OutgoingCommand[CancelError \/ Unit] =
        outgoingCommand[BookingCommand, CancelError \/ Unit](Cancel)

      override def notifyCapacity(
          isAvailable: Boolean
      ): OutgoingCommand[BookingAlg.BookingUnknown.type \/ Unit] =
        outgoingCommand[BookingCommand, BookingUnknown.type \/ Unit](NotifyCapacity(isAvailable))
    }

//#example-server
  override def server[F[_]]: Decoder[IncomingCommand[F, BookingAlg]] =
    CirceDecoder(io.circe.Decoder[BookingCommand].map {
      case PlaceBooking(
            bookingID: BookingID,
            time: Instant,
            passengerCount: Int,
            origin: LatLon,
            destination: LatLon
          ) =>
        incomingCommand[F, BookingAlreadyExists \/ Unit](
          _.place(bookingID, time, passengerCount, origin, destination)
        )
      // #example-server

      case Get => incomingCommand[F, BookingUnknown.type \/ Booking](_.get)
      case ChangeOrigin(newOrigin) =>
        incomingCommand[F, BookingUnknown.type \/ Unit](_.changeOrigin(newOrigin))
      case ChangeDestination(newDestination) =>
        incomingCommand[F, BookingUnknown.type \/ Unit](_.changeDestination(newDestination))
      case ChangeOriginAndDestination(newOrigin, newDestination) =>
        incomingCommand[F, BookingUnknown.type \/ Unit](
          _.changeOriginAndDestination(newOrigin, newDestination)
        )
      case Cancel => incomingCommand[F, CancelError \/ Unit](_.cancel)
      case NotifyCapacity(isAvailable) =>
        incomingCommand[F, BookingUnknown.type \/ Unit](_.notifyCapacity(isAvailable))
    })
}
