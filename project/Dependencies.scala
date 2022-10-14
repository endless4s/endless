import sbt._

object Dependencies {
  lazy val akkaVersion = "2.6.17"
  lazy val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed"
  lazy val akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed"
  lazy val akkaClusterTyped = "com.typesafe.akka" %% "akka-cluster-typed"
  lazy val akkaClusterShardingTyped = "com.typesafe.akka" %% "akka-cluster-sharding-typed"

  lazy val akkaTypedTestkit = "com.typesafe.akka" %% "akka-actor-testkit-typed"
  lazy val akkaPersistenceTestkit = "com.typesafe.akka" %% "akka-persistence-testkit"

  lazy val akka =
    Seq(
      akkaActorTyped,
      akkaClusterTyped,
      akkaClusterShardingTyped,
      akkaPersistenceTyped
    ).map(_ % akkaVersion)

  lazy val akkaProvided = akka.map(_ % Provided)

  lazy val akkaTest = Seq(akkaPersistenceTestkit).map(_ % akkaVersion)

  lazy val catsVersion = "2.8.0"
  lazy val cats =
    Seq(
      "org.typelevel" %% "cats-core",
      "org.typelevel" %% "cats-kernel"
    ).map(_ % catsVersion)

  lazy val catsLaws = Seq("org.typelevel" %% "cats-laws" % catsVersion)
  lazy val catsTestkit = Seq("org.typelevel" %% "cats-testkit" % catsVersion)
  lazy val catsTaglessVersion = "0.14.0"
  lazy val catsTagless = Seq("org.typelevel" %% "cats-tagless-macros" % catsTaglessVersion)

  lazy val catsEffectVersion = "3.3.14"
  lazy val catsEffectKernel = Seq("org.typelevel" %% "cats-effect-kernel" % catsEffectVersion)
  lazy val catsEffectLaws = Seq("org.typelevel" %% "cats-effect-laws" % catsEffectVersion)
  lazy val catsEffectTestKit = Seq("org.typelevel" %% "cats-effect-testkit" % catsEffectVersion)
  lazy val catsEffectStd = Seq("org.typelevel" %% "cats-effect-std" % catsEffectVersion)
  lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % catsEffectVersion)

  lazy val http4sVersion = "0.23.15"
  lazy val http4s = Seq(
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion
  )

  lazy val blazeVersion = "0.23.12"
  lazy val blaze = Seq(
    "org.http4s" %% "http4s-blaze-server" % blazeVersion,
    "org.http4s" %% "http4s-blaze-client" % blazeVersion
  )

  lazy val circeVersion = "0.14.2"
  lazy val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)

  lazy val logbackVersion = "1.4.1"
  lazy val logback = Seq("ch.qos.logback" % "logback-classic" % logbackVersion)

  lazy val log4catsVersion = "2.5.0"
  lazy val log4cats = Seq("org.typelevel" %% "log4cats-core" % log4catsVersion)
  lazy val log4catsSlf4j = Seq(
    "org.typelevel" %% "log4cats-slf4j" % log4catsVersion
  )
  lazy val log4catsTesting = Seq("org.typelevel" %% "log4cats-testing" % log4catsVersion)

  lazy val mUnitVersion = "0.7.29"
  lazy val disciplineMUnitVersion = "1.0.9"
  lazy val mUnit =
    Seq("org.scalameta" %% "munit", "org.scalameta" %% "munit-scalacheck").map(
      _ % mUnitVersion
    ) ++ Seq("org.typelevel" %% "discipline-munit" % disciplineMUnitVersion)

  lazy val catsEffectMUnitVersion = "1.0.7"
  lazy val catsEffectMUnit = Seq("org.typelevel" %% "munit-cats-effect-3" % catsEffectMUnitVersion)

  lazy val scalacheckEffectVersion = "1.0.4"
  lazy val scalacheckEffect = Seq(
    "org.typelevel" %% "scalacheck-effect-munit" % scalacheckEffectVersion
  )

  lazy val kittensVersion = "2.3.2"
  lazy val kittens = Seq("org.typelevel" %% "kittens" % kittensVersion)

  lazy val scodecCore = Seq("org.scodec" %% "scodec-core" % "1.11.10")

  lazy val scalapbCustomizations = Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  )
}
