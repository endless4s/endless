package endless.example.protocol

import cats.conversions.all._
import com.google.protobuf.timestamp.Timestamp
import endless.\/
import endless.core.protocol.{CommandSender, Decoder, IncomingCommand}
import endless.example.algebra.BookingAlg
import endless.example.algebra.BookingAlg._
import endless.example.data.Booking.BookingID
import endless.example.data._
import endless.example.proto.booking.commands.BookingCommand.Command
import endless.example.proto.booking.commands._
import endless.example.proto.booking.commands.{BookingCommand, PlaceBookingV1}
import endless.example.proto.booking.models.BookingStatusV1.Status
import endless.example.proto.booking.replies
import endless.example.proto.booking.{models => proto}
import endless.protobuf.{ProtobufCommandProtocol, ProtobufDecoder}
import BookingCommandProtocol._
import endless.example.proto.booking.replies.UnitReply

import java.time.Instant
import java.util.UUID

//#example-client
class BookingCommandProtocol extends ProtobufCommandProtocol[BookingID, BookingAlg] {
  override def clientFor[F[_]](
      id: BookingID
  )(implicit sender: CommandSender[F, BookingID]): BookingAlg[F] =
    new BookingAlg[F] {
      def place(
          bookingID: BookingID,
          time: Instant,
          passengerCount: Int,
          origin: LatLon,
          destination: LatLon
      ): F[BookingAlreadyExists \/ Unit] =
        sendCommand[F, BookingCommand, replies.PlaceBookingReply, BookingAlreadyExists \/ Unit](
          id,
          BookingCommand.of(
            Command.PlaceBookingV1(
              PlaceBookingV1(
                proto.BookingID(bookingID.id.toString),
                Timestamp.of(time.getEpochSecond, time.getNano),
                passengerCount,
                proto.LatLonV1(origin.lat, origin.lon),
                proto.LatLonV1(destination.lat, destination.lon)
              )
            )
          ),
          {
            case replies
                  .PlaceBookingReply(replies.PlaceBookingReply.Reply.AlreadyExists(booking), _) =>
              Left(BookingAlreadyExists(BookingID(UUID.fromString(booking.bookingId.value))))
            case replies.PlaceBookingReply(replies.PlaceBookingReply.Reply.Unit(_), _) => Right(())
            case replies.PlaceBookingReply(replies.PlaceBookingReply.Reply.Empty, _) =>
              throw new UnexpectedReplyException
          }
        )

      // ...
      // #example-client

      def get: F[BookingUnknown.type \/ Booking] =
        sendCommand[F, BookingCommand, replies.GetBookingReply, BookingUnknown.type \/ Booking](
          id,
          BookingCommand.of(Command.GetBookingV1(GetBookingV1())),
          {
            case replies.GetBookingReply(replies.GetBookingReply.Reply.Booking(booking), _) =>
              Right(
                Booking(
                  BookingID(UUID.fromString(booking.id.value)),
                  Instant.ofEpochSecond(booking.time.seconds, booking.time.nanos),
                  LatLon(booking.origin.lat, booking.origin.lon),
                  LatLon(booking.destination.lat, booking.destination.lon),
                  booking.passengerCount,
                  booking.status.value match {
                    case Status.PENDING         => Booking.Status.Pending
                    case Status.ACCEPTED        => Booking.Status.Accepted
                    case Status.REJECTED        => Booking.Status.Rejected
                    case Status.CANCELLED       => Booking.Status.Cancelled
                    case Status.Unrecognized(_) => throw new UnexpectedReplyException
                  }
                )
              )
            case replies.GetBookingReply(replies.GetBookingReply.Reply.Unknown(_), _) =>
              Left(BookingUnknown)
            case replies.GetBookingReply(replies.GetBookingReply.Reply.Empty, _) =>
              throw new UnexpectedReplyException
          }
        )

      def changeOrigin(
          newOrigin: LatLon
      ): F[BookingUnknown.type \/ Unit] =
        sendCommand[F, BookingCommand, replies.ChangeOriginReply, BookingUnknown.type \/ Unit](
          id,
          BookingCommand.of(
            Command.ChangeOriginV1(ChangeOriginV1(proto.LatLonV1(newOrigin.lat, newOrigin.lon)))
          ),
          {
            case replies.ChangeOriginReply(replies.ChangeOriginReply.Reply.Unit(_), _) => Right(())
            case replies.ChangeOriginReply(replies.ChangeOriginReply.Reply.Unknown(_), _) =>
              Left(BookingUnknown)
            case replies.ChangeOriginReply(replies.ChangeOriginReply.Reply.Empty, _) =>
              throw new UnexpectedReplyException
          }
        )

      def changeDestination(
          newDestination: LatLon
      ): F[BookingUnknown.type \/ Unit] =
        sendCommand[F, BookingCommand, replies.ChangeDestinationReply, BookingUnknown.type \/ Unit](
          id,
          BookingCommand.of(
            Command.ChangeDestinationV1(
              ChangeDestinationV1(proto.LatLonV1(newDestination.lat, newDestination.lon))
            )
          ),
          {
            case replies.ChangeDestinationReply(replies.ChangeDestinationReply.Reply.Unit(_), _) =>
              Right(())
            case replies
                  .ChangeDestinationReply(replies.ChangeDestinationReply.Reply.Unknown(_), _) =>
              Left(BookingUnknown)
            case replies.ChangeDestinationReply(replies.ChangeDestinationReply.Reply.Empty, _) =>
              throw new UnexpectedReplyException
          }
        )

      def changeOriginAndDestination(
          newOrigin: LatLon,
          newDestination: LatLon
      ): F[BookingUnknown.type \/ Unit] =
        sendCommand[
          F,
          BookingCommand,
          replies.ChangeOriginAndDestinationReply,
          BookingUnknown.type \/ Unit
        ](
          id,
          BookingCommand.of(
            Command.ChangeOriginAndDestinationV1(
              ChangeOriginAndDestinationV1(
                proto.LatLonV1(newOrigin.lat, newOrigin.lon),
                proto.LatLonV1(newDestination.lat, newDestination.lon)
              )
            )
          ),
          {
            case replies.ChangeOriginAndDestinationReply(
                  replies.ChangeOriginAndDestinationReply.Reply.Unit(_),
                  _
                ) =>
              Right(())
            case replies.ChangeOriginAndDestinationReply(
                  replies.ChangeOriginAndDestinationReply.Reply.Unknown(_),
                  _
                ) =>
              Left(BookingUnknown)
            case replies.ChangeOriginAndDestinationReply(
                  replies.ChangeOriginAndDestinationReply.Reply.Empty,
                  _
                ) =>
              throw new UnexpectedReplyException
          }
        )

      override def cancel: F[CancelError \/ Unit] =
        sendCommand[F, BookingCommand, replies.CancelBookingReply, CancelError \/ Unit](
          id,
          BookingCommand.of(Command.CancelBookingV1(CancelBookingV1())),
          {
            case replies.CancelBookingReply(replies.CancelBookingReply.Reply.Unit(_), _) =>
              Right(())
            case replies.CancelBookingReply(replies.CancelBookingReply.Reply.Unknown(_), _) =>
              Left(BookingUnknown)
            case replies.CancelBookingReply(
                  replies.CancelBookingReply.Reply
                    .Rejected(replies.BookingWasRejectedV1(bookingID, _)),
                  _
                ) =>
              Left(BookingWasRejected(BookingID(UUID.fromString(bookingID.value))))
            case replies.CancelBookingReply(replies.CancelBookingReply.Reply.Empty, _) =>
              throw new UnexpectedReplyException
          }
        )

      override def notifyCapacity(
          isAvailable: Boolean
      ): F[BookingAlg.BookingUnknown.type \/ Unit] =
        sendCommand[F, BookingCommand, replies.NotifyCapacityReply, BookingUnknown.type \/ Unit](
          id,
          BookingCommand.of(Command.NotifyCapacityV1(NotifyCapacityV1(isAvailable))),
          {
            case replies.NotifyCapacityReply(replies.NotifyCapacityReply.Reply.Unit(_), _) =>
              Right(())
            case replies.NotifyCapacityReply(replies.NotifyCapacityReply.Reply.Unknown(_), _) =>
              Left(BookingUnknown)
            case replies.NotifyCapacityReply(replies.NotifyCapacityReply.Reply.Empty, _) =>
              throw new UnexpectedReplyException
          }
        )
    }

  // #example-server
  override def server[F[_]]: Decoder[IncomingCommand[F, BookingAlg]] =
    ProtobufDecoder[BookingCommand].map(_.command match {
      case Command.Empty => throw new UnexpectedCommandException
      case Command.PlaceBookingV1(
            PlaceBookingV1(bookingID, time, passengerCount, origin, destination, _)
          ) =>
        handleCommand[F, replies.PlaceBookingReply, BookingAlreadyExists \/ Unit](
          _.place(
            BookingID(UUID.fromString(bookingID.value)),
            Instant.ofEpochSecond(time.seconds, time.nanos),
            passengerCount,
            LatLon(origin.lat, origin.lon),
            LatLon(destination.lat, destination.lon)
          ),
          {
            case Left(bookingAlreadyExists) =>
              replies.PlaceBookingReply(
                replies.PlaceBookingReply.Reply.AlreadyExists(
                  replies.BookingAlreadyExistsV1(
                    proto.BookingID(bookingAlreadyExists.bookingID.id.toString)
                  )
                )
              )
            case Right(_) =>
              replies.PlaceBookingReply(replies.PlaceBookingReply.Reply.Unit(UnitReply()))
          }
        )
      // #example-server

      case Command.GetBookingV1(_) =>
        handleCommand[F, replies.GetBookingReply, BookingUnknown.type \/ Booking](
          _.get,
          {
            case Left(BookingUnknown) =>
              replies.GetBookingReply(
                replies.GetBookingReply.Reply.Unknown(replies.BookingUnknown())
              )
            case Right(booking) =>
              replies.GetBookingReply(
                replies.GetBookingReply.Reply.Booking(
                  proto.BookingV1(
                    proto.BookingID(booking.id.id.toString),
                    Timestamp(booking.time.getEpochSecond, booking.time.getNano),
                    proto.LatLonV1(booking.origin.lat, booking.origin.lon),
                    proto.LatLonV1(booking.destination.lat, booking.destination.lon),
                    booking.passengerCount,
                    proto.BookingStatusV1(booking.status match {
                      case Booking.Status.Pending   => proto.BookingStatusV1.Status.PENDING
                      case Booking.Status.Accepted  => proto.BookingStatusV1.Status.ACCEPTED
                      case Booking.Status.Rejected  => proto.BookingStatusV1.Status.REJECTED
                      case Booking.Status.Cancelled => proto.BookingStatusV1.Status.CANCELLED
                    })
                  )
                )
              )
          }
        )

      case Command.ChangeOriginV1(ChangeOriginV1(newOrigin, _)) =>
        handleCommand[F, replies.ChangeOriginReply, BookingUnknown.type \/ Unit](
          _.changeOrigin(LatLon(newOrigin.lat, newOrigin.lon)),
          {
            case Left(BookingUnknown) =>
              replies.ChangeOriginReply(
                replies.ChangeOriginReply.Reply.Unknown(replies.BookingUnknown())
              )
            case Right(_) =>
              replies.ChangeOriginReply(replies.ChangeOriginReply.Reply.Unit(UnitReply()))
          }
        )

      case Command.ChangeDestinationV1(ChangeDestinationV1(newDestination, _)) =>
        handleCommand[F, replies.ChangeDestinationReply, BookingUnknown.type \/ Unit](
          _.changeDestination(LatLon(newDestination.lat, newDestination.lon)),
          {
            case Left(BookingUnknown) =>
              replies.ChangeDestinationReply(
                replies.ChangeDestinationReply.Reply.Unknown(replies.BookingUnknown())
              )
            case Right(_) =>
              replies.ChangeDestinationReply(replies.ChangeDestinationReply.Reply.Unit(UnitReply()))
          }
        )

      case Command.ChangeOriginAndDestinationV1(
            ChangeOriginAndDestinationV1(newOrigin, newDestination, _)
          ) =>
        handleCommand[F, replies.ChangeOriginAndDestinationReply, BookingUnknown.type \/ Unit](
          _.changeOriginAndDestination(
            LatLon(newOrigin.lat, newOrigin.lon),
            LatLon(newDestination.lat, newDestination.lon)
          ),
          {
            case Left(BookingUnknown) =>
              replies.ChangeOriginAndDestinationReply(
                replies.ChangeOriginAndDestinationReply.Reply.Unknown(replies.BookingUnknown())
              )
            case Right(_) =>
              replies.ChangeOriginAndDestinationReply(
                replies.ChangeOriginAndDestinationReply.Reply.Unit(UnitReply())
              )
          }
        )

      case Command.CancelBookingV1(_) =>
        handleCommand[F, replies.CancelBookingReply, CancelError \/ Unit](
          _.cancel,
          {
            case Left(BookingUnknown) =>
              replies.CancelBookingReply(
                replies.CancelBookingReply.Reply.Unknown(replies.BookingUnknown())
              )
            case Left(BookingWasRejected(bookingID)) =>
              replies.CancelBookingReply(
                replies.CancelBookingReply.Reply.Rejected(
                  replies.BookingWasRejectedV1(proto.BookingID(bookingID.id.toString))
                )
              )
            case Right(_) =>
              replies.CancelBookingReply(replies.CancelBookingReply.Reply.Unit(UnitReply()))
          }
        )

      case Command.NotifyCapacityV1(NotifyCapacityV1(isAvailable, _)) =>
        handleCommand[F, replies.NotifyCapacityReply, BookingUnknown.type \/ Unit](
          _.notifyCapacity(isAvailable),
          {
            case Left(BookingUnknown) =>
              replies.NotifyCapacityReply(
                replies.NotifyCapacityReply.Reply.Unknown(replies.BookingUnknown())
              )
            case Right(_) =>
              replies.NotifyCapacityReply(replies.NotifyCapacityReply.Reply.Unit(UnitReply()))
          }
        )
    })
}

object BookingCommandProtocol {
  final class UnexpectedCommandException extends RuntimeException("Unexpected command")
  final class UnexpectedReplyException extends RuntimeException("Unexpected reply")
}
