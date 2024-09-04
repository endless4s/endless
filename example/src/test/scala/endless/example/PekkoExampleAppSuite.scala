package endless.example

import endless.example.app.pekko.PekkoApp
import munit.AnyFixture

class PekkoExampleAppSuite extends munit.CatsEffectSuite with ExampleAppSuite {
  lazy val port: Int = 8081
  private val pekkoServer = ResourceSuiteLocalFixture("pekko-server", PekkoApp(port))
  override def munitFixtures: Seq[AnyFixture[?]] = List(pekkoServer, client)
}
