package endless.example.protocol

import endless.\/
import endless.circe.{CirceCommandProtocol, CirceDecoder}
import endless.core.typeclass.protocol.{Decoder, IncomingCommand, OutgoingCommand}
import endless.example.algebra.BookingAlg
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown}
import endless.example.data.Booking
import endless.example.data.Booking.{BookingID, LatLon}
import endless.example.protocol.BookingCommand._
import io.circe.generic.auto._

class BookingCommandProtocol extends CirceCommandProtocol[BookingAlg] {
  override def client: BookingAlg[OutgoingCommand[*]] =
    new BookingAlg[OutgoingCommand[*]] {
      def place(
          bookingID: BookingID,
          passengerCount: Int,
          origin: LatLon,
          destination: LatLon
      ): OutgoingCommand[BookingAlreadyExists \/ Unit] =
        outgoingCommand[BookingCommand, BookingAlreadyExists \/ Unit](
          PlaceBooking(bookingID, passengerCount, origin, destination)
        )

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
    }

  override def server[F[_]]: Decoder[IncomingCommand[F, BookingAlg]] =
    CirceDecoder(io.circe.Decoder[BookingCommand].map {
      case PlaceBooking(
            rideID: BookingID,
            passengerCount: Int,
            origin: LatLon,
            destination: LatLon
          ) =>
        incomingCommand[F, BookingAlreadyExists \/ Unit](
          _.place(rideID, passengerCount, origin, destination)
        )
      case Get => incomingCommand[F, BookingUnknown.type \/ Booking](_.get)
      case ChangeOrigin(newOrigin) =>
        incomingCommand[F, BookingUnknown.type \/ Unit](_.changeOrigin(newOrigin))
      case ChangeDestination(newDestination) =>
        incomingCommand[F, BookingUnknown.type \/ Unit](_.changeDestination(newDestination))
      case ChangeOriginAndDestination(newOrigin, newDestination) =>
        incomingCommand[F, BookingUnknown.type \/ Unit](
          _.changeOriginAndDestination(newOrigin, newDestination)
        )
    })
}
