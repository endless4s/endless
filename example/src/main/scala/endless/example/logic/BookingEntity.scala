package endless.example.logic

import cats.Monad
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.eq._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import endless.\/
import endless.core.typeclass.entity.Entity
import endless.example.algebra.BookingAlg
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown}
import endless.example.data.Booking._
import endless.example.data.BookingEvent._
import endless.example.data.{Booking, BookingEvent}
import org.typelevel.log4cats.Logger

final case class BookingEntity[F[_]: Monad: Logger](entity: Entity[F, Booking, BookingEvent])
    extends BookingAlg[F] {
  import entity._

  def place(
      bookingID: BookingID,
      passengerCount: Int,
      origin: LatLon,
      destination: LatLon
  ): F[BookingAlreadyExists \/ Unit] =
    ifUnknown(
      Logger[F].info(show"Creating booking with ID $bookingID") >> write(
        BookingPlaced(bookingID, origin, destination, passengerCount)
      )
    )(_ => BookingAlreadyExists(bookingID))

  def get: F[BookingUnknown.type \/ Booking] = ifKnown(_.pure)(BookingUnknown)

  def changeOrigin(newOrigin: LatLon): F[BookingUnknown.type \/ Unit] =
    ifKnown(booking =>
      if (booking.origin =!= newOrigin) entity.write(OriginChanged(newOrigin)) else ().pure
    )(BookingUnknown)

  def changeDestination(newDestination: LatLon): F[BookingUnknown.type \/ Unit] =
    ifKnown(booking =>
      if (booking.destination =!= newDestination) entity.write(DestinationChanged(newDestination))
      else ().pure
    )(BookingUnknown)

  def changeOriginAndDestination(
      newOrigin: LatLon,
      newDestination: LatLon
  ): F[BookingUnknown.type \/ Unit] = changeOrigin(newOrigin) >> changeDestination(newDestination)

}
