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

final case class BookingEntity[F[_]: Monad: Logger](
    entity: Entity[F, Option[Booking], BookingEvent]
) extends BookingAlg[F] {
  import entity._

  def place(
      bookingID: BookingID,
      passengerCount: Int,
      origin: LatLon,
      destination: LatLon
  ): F[BookingAlreadyExists \/ Unit] =
    read >>= {
      case Some(_) => BookingAlreadyExists(bookingID).asLeft.pure
      case None =>
        Logger[F].info(show"Creating booking with ID $bookingID") >> write(
          BookingPlaced(bookingID, origin, destination, passengerCount)
        )
          .map(_.asRight)
    }

  def get: F[BookingUnknown.type \/ Booking] = ifKnown(_.pure)

  def changeOrigin(newOrigin: LatLon): F[BookingUnknown.type \/ Unit] =
    ifKnown(booking =>
      if (booking.origin =!= newOrigin) entity.write(OriginChanged(newOrigin)) else ().pure
    )

  def changeDestination(newDestination: LatLon): F[BookingUnknown.type \/ Unit] =
    ifKnown(booking =>
      if (booking.destination =!= newDestination) entity.write(DestinationChanged(newDestination))
      else ().pure
    )

  def changeOriginAndDestination(
      newOrigin: LatLon,
      newDestination: LatLon
  ): F[BookingUnknown.type \/ Unit] = changeOrigin(newOrigin) >> changeDestination(newDestination)

  private def ifKnown[A](fa: Booking => F[A]): F[BookingUnknown.type \/ A] =
    read >>= {
      case Some(booking) => fa(booking).map(_.asRight)
      case None          => BookingUnknown.asLeft.pure
    }
}
