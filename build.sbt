import Dependencies._

val commonSettings = Seq(
  Compile / scalacOptions --= Seq("-language:implicitConversions", "-Xsource:2.14"),
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.1" cross CrossVersion.full),
  wartremoverExcluded += sourceManaged.value,
  Compile / wartremoverErrors ++= Warts
    .allBut(Wart.Any, Wart.Nothing, Wart.ImplicitParameter, Wart.Throw),
  coverageExcludedPackages := "<empty>;endless.test.*"
)

inThisBuild(
  List(
    organization := "io.github.endless4s",
    homepage := Some(url("https://github.com/endless4s/endless")),
    licenses := List("MIT License" -> url("http://opensource.org/licenses/mit-license.php")),
    developers := List(
      Developer(
        "jchapuis",
        "Jonas Chapuis",
        "me@jonaschapuis.com",
        url("https://jonaschapuis.com")
      )
    ),
    scalaVersion := "2.13.3",
    Global / onChangedBuildSource := ReloadOnSourceChanges
  )
)

lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= cats ++ catsTagless)
  .settings(name := "endless-core")

lazy val runtime = (project in file("runtime"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= catsEffectStd ++ akka)
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    )
  )
  .settings(name := "endless-runtime-akka")

lazy val circeHelpers = (project in file("circe"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= circe :+ (akkaActorTyped % akkaVersion))
  .settings(name := "endless-circe-helpers")

lazy val example = (project in file("example"))
  .dependsOn(core, runtime, circeHelpers)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= catsEffect ++ http4s ++ akkaTest ++ logback)
  .settings(name := "endless-example", run / fork := true)

lazy val root = project
  .in(file("."))
  .aggregate(core, runtime, circeHelpers, example)
  .dependsOn(example)
  .settings(Compile / mainClass := (example / Compile / mainClass).value)
  .settings(commonSettings: _*)
  .settings(
    name := "endless",
    version := "0.0.1"
  )
