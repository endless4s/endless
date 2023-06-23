package endless.example.app.pekko

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = PekkoApp(8080).useForever.as(ExitCode.Success)
}
