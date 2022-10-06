package endless.example.data

import cats.Show
import cats.syntax.show._

final case class Speed(metersPerSecond: Double) extends AnyVal

object Speed {
  implicit val show: Show[Speed] = Show.show(speed => show"${speed.metersPerSecond} m/s")
}
