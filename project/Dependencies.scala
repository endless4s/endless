import sbt._

object Dependencies {
  lazy val akkaVersion = "2.6.17"
  lazy val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed"
  lazy val akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed"
  lazy val akkaClusterTyped = "com.typesafe.akka" %% "akka-cluster-typed"
  lazy val akkaClusterShardingTyped =
    "com.typesafe.akka" %% "akka-cluster-sharding-typed"
  lazy val akkaPersistenceTestkit = "com.typesafe.akka" %% "akka-persistence-testkit"

  lazy val akka =
    Seq(
      akkaActorTyped,
      akkaClusterTyped,
      akkaClusterShardingTyped,
      akkaPersistenceTyped
    ).map(_ % akkaVersion)

  lazy val akkaTest = Seq(akkaPersistenceTestkit).map(_ % akkaVersion)

  lazy val catsVersion = "2.6.1"
  lazy val cats =
    Seq(
      "org.typelevel" %% "cats-core",
      "org.typelevel" %% "cats-kernel"
    ).map(_ % catsVersion)

  lazy val catsLaws = Seq("org.typelevel" %% "cats-laws" % catsVersion)
  lazy val catsTestkit = Seq("org.typelevel" %% "cats-testkit" % catsVersion)
  lazy val catsTaglessVersion = "0.14.0"
  lazy val catsTagless = Seq("org.typelevel" %% "cats-tagless-macros" % catsTaglessVersion)

  lazy val catsEffectVersion = "3.2.9"
  lazy val catsEffectStd = Seq("org.typelevel" %% "cats-effect-std" % catsEffectVersion)
  lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % catsEffectVersion)

  lazy val http4sVersion = "0.23.6"
  lazy val http4s = Seq(
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion
  )

  lazy val circeVersion = "0.14.1"
  lazy val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)

  lazy val logbackVersion = "1.2.6"
  lazy val logback = Seq("ch.qos.logback" % "logback-classic" % logbackVersion)

  lazy val log4catsVersion = "2.1.1"
  lazy val log4cats = Seq("org.typelevel" %% "log4cats-core" % log4catsVersion)
  lazy val log4catsSlf4j = Seq(
    "org.typelevel" %% "log4cats-slf4j" % log4catsVersion
  )

  lazy val mUnitVersion = "0.7.29"
  lazy val disciplineMUnitVersion = "1.0.9"
  lazy val mUnit =
    Seq("org.scalameta" %% "munit", "org.scalameta" %% "munit-scalacheck").map(
      _ % mUnitVersion
    ) ++ Seq("org.typelevel" %% "discipline-munit" % disciplineMUnitVersion)

  lazy val catsEffectMUnitVersion = "1.0.6"
  lazy val catsEffectMUnit = Seq("org.typelevel" %% "munit-cats-effect-3" % catsEffectMUnitVersion)

}
