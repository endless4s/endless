package endless.example.data

import cats.Show
import cats.kernel.Eq
import cats.syntax.show.*

final case class LatLon(lat: Double, lon: Double)

object LatLon {
  implicit val eq: Eq[LatLon] = Eq.fromUniversalEquals
  implicit val show: Show[LatLon] =
    Show.show(latLon => show"[lat=${latLon.lat}, lon=${latLon.lon}]")
}
