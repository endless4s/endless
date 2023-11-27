package endless.example.app.akka

import cats.effect.*

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = AkkaApp(8080).useForever.as(ExitCode.Success)
}
