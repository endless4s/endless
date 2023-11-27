package endless.example

import endless.example.app.akka.AkkaApp

class AkkaExampleAppSuite extends munit.CatsEffectSuite with ExampleAppSuite {
  lazy val port: Int = 8080
  private val akkaServer = ResourceSuiteLocalFixture("akka-server", AkkaApp(port))
  override def munitFixtures: Seq[Fixture[?]] = List(akkaServer, client)
}
