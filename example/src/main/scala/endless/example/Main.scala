package endless.example

import akka.actor.typed.ActorSystem
import akka.persistence.testkit.{
  PersistenceTestKitDurableStateStorePlugin,
  PersistenceTestKitPlugin
}
import cats.effect._
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  def actorSystem(executionContext: ExecutionContext): ActorSystem[Nothing] =
    ActorSystem.wrap(
      akka.actor.ActorSystem(
        name = "example-as",
        config = Some(
          PersistenceTestKitPlugin.config
            .withFallback(PersistenceTestKitDurableStateStorePlugin.config)
            .withFallback(ConfigFactory.defaultApplication)
            .resolve()
        ),
        defaultExecutionContext = Some(executionContext),
        classLoader = None
      )
    )

  def run(args: List[String]): IO[ExitCode] = IO.executionContext
    .map(actorSystem)
    .flatMap(system => ExampleApp.apply(system).useForever.as(ExitCode.Success))

}
