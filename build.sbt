import Dependencies._

val commonSettings = Seq(
  Compile / scalacOptions --= Seq("-language:implicitConversions", "-Xsource:2.14"),
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.1" cross CrossVersion.full),
  wartremoverExcluded += sourceManaged.value,
  Compile / wartremoverErrors ++= Warts.all,
  coverageExcludedPackages := "<empty>;akka.persistence.tagless.test.*"
)

inThisBuild(
  List(
    scalaVersion := "2.13.3",
    Global / onChangedBuildSource := ReloadOnSourceChanges
  )
)

lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= cats ++ catsTagless)
  .settings(name := "akka-persistence-tagless-core")

lazy val runtime = (project in file("runtime"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= catsEffect ++ akka)
  .settings(name := "akka-persistence-tagless-runtime")

lazy val example = (project in file("example"))
  .dependsOn(core, runtime)
  .settings(commonSettings: _*)
  .settings(name := "akka-persistence-tagless-example")

lazy val root = project
  .in(file("."))
  .aggregate(core, runtime, example)
  .settings(commonSettings: _*)
  .settings(
    name := "akka-persistence-tagless",
    version := "0.0.1"
  )
