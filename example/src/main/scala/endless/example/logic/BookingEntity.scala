package endless.example.logic

import cats.data.EitherT
import cats.effect.kernel.Clock
import cats.syntax.applicative._
import cats.syntax.eq._
import cats.syntax.flatMap._
import cats.syntax.show._
import endless.\/
import endless.core.entity.Entity
import endless.example.algebra.BookingAlg
import endless.example.algebra.BookingAlg.{BookingAlreadyExists, BookingUnknown, CancelError}
import endless.example.data.Booking._
import endless.example.data.BookingEvent._
import endless.example.data.{Booking, BookingEvent, LatLon}
import org.typelevel.log4cats.Logger

import java.time.Instant

//#definition
final case class BookingEntity[F[_]: Logger: Clock](entity: Entity[F, Booking, BookingEvent])
    extends BookingAlg[F] {
  import entity._

  def place(
      bookingID: BookingID,
      time: Instant,
      passengerCount: Int,
      origin: LatLon,
      destination: LatLon
  ): F[BookingAlreadyExists \/ Unit] = ???

  def get: F[BookingUnknown.type \/ Booking] = ???

  def changeOrigin(newOrigin: LatLon): F[BookingUnknown.type \/ Unit] = ???

  def changeDestination(newDestination: LatLon): F[BookingUnknown.type \/ Unit] = ???

  def changeOriginAndDestination(
      newOrigin: LatLon,
      newDestination: LatLon
  ): F[BookingUnknown.type \/ Unit] = ???

  def cancel: F[CancelError \/ Unit] = ???

  def notifyCapacity(isAvailable: Boolean): F[BookingAlg.BookingUnknown.type \/ Unit] = ???
}
//#definition
