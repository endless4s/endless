package akka.persistence.tagless.example

import akka.persistence.tagless.\/
import akka.persistence.tagless.core.typeclass.{Entity, Repository}
import cats.Monad
import cats.data.NonEmptyList
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._

case class Booking(
    clientId: ClientId,
    concertId: ConcertId,
    seats: NonEmptyList[Seat],
    status: BookingStatus
)

sealed trait BookingEvent
object BookingEvent {
  case class BookingPlaced(
      clientID: ClientId,
      concertId: ConcertId,
      seats: NonEmptyList[Seat]
  ) extends BookingEvent
}

case class ClientId(id: String)
case class ConcertId(id: String)
case class Seat(number: Int)
sealed trait BookingStatus
object BookingStatus {
  object Pending extends BookingStatus
  object Paid extends BookingStatus
  object None extends BookingStatus
}

case class BookingAlreadyExists(clientId: ClientId, concertId: ConcertId)

trait BookingAlg[F[_]] {
  def place(
      clientId: ClientId,
      concertId: ConcertId,
      seats: NonEmptyList[Seat]
  ): F[BookingAlreadyExists \/ Unit]
  def status: F[BookingStatus]
}

trait BookingRepositoryAlg[F[_]] {
  def bookingFor(clientId: ClientId): BookingAlg[F]
}

class BookingRepository[F[_]: Monad](implicit repository: Repository[F, ClientId, BookingAlg])
    extends BookingRepositoryAlg[F] {
  import repository._
  def bookingFor(clientId: ClientId): BookingAlg[F] = entityFor(clientId)
}

class BookingEntity[F[_]: Monad](implicit entity: Entity[F, Option[Booking], BookingEvent])
    extends BookingAlg[F] {
  import entity._

  def place(
      clientId: ClientId,
      concertId: ConcertId,
      seats: NonEmptyList[Seat]
  ): F[BookingAlreadyExists \/ Unit] =
    read >>= {
      case Some(_) =>
        write(BookingEvent.BookingPlaced(clientId, concertId, seats)).map(_.asRight)
      case None => BookingAlreadyExists(clientId, concertId).asLeft.pure
    }

  def status: F[BookingStatus] = read >>= {
    case Some(booking) => booking.status.pure
    case None          => BookingStatus.None.pure[F]
  }
}

//
//class BookingCommandProtocol[F[_]] extends CommandProtocol[BookingRepository, String] {
//  override def client: BookingRepository[Encoded[String, _]] =
//    BookingRepository[Encoded[String, _]] {
//      def place(
//                 clientId: ClientId,
//                 concertId: ConcertId,
//                 seats: NonEmptyList[Seat]
//               ): Encoded[String, BookingAlreadyExists \/ Unit] =
//        new Encoded[String, BookingAlreadyExists \/ Unit]:
//
//      override def payload: String = clientId.id + concertId.id
//
//      override def decoder: Decoder[String, BookingAlreadyExists \/ Unit] =
//        new Decoder[String, BookingAlreadyExists \/ Unit]:
//
//      override def decode(
//                           encoded: Encoded[String, BookingAlreadyExists \/ Unit]
//                         ): BookingAlreadyExists \/ Unit = ().asRight
//
//      def status(clientId: ClientId): Encoded[String, _][BookingStatus] = ???
//    }
//
//  override def server: Decoder[CommandMessage[Booking, String], String] = ???
//}
// a la endpoints ?
//trait BookingRepository:
//  def place : Command[(ClientId, ConcertId, NonEmptyList[Seat]), Either[PlaceError, Unit]] = command()
//  def status: Command[Unit, Either[StatusError, Status]] = command()
//

// wrap response types into some [Wire[A]]
// form tuples from repository algebra
// entity is a traverse operation on commands, each generating events

//trait class EntityT[F[_], S, E, A] private (
//                                             val unsafeRun: (S, (S, E) => Folded[S], Chain[E]) => F[Folded[(Chain[E], A)]]
//                                           ) extends AnyVal {
//
//  def run(current: S, update: (S, E) => Folded[S]): F[Folded[(Chain[E], A)]] =
//    unsafeRun(current, update, Chain.empty)
