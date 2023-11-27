package endless.example.data

import cats.Show
import cats.syntax.show.*

final case class Speed(metersPerSecond: Double)

object Speed {
  implicit val show: Show[Speed] = Show.show(speed => show"${speed.metersPerSecond} m/s")
}
