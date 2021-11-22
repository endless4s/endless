package endless.example.algebra

import java.time.Instant

trait AvailabilityAlg[F[_]] {
  def isCapacityAvailable(time: Instant, passengerCount: Int): F[Boolean]
}
