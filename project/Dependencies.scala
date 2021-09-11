import sbt._

object Dependencies {
  lazy val akkaVersion = "2.6.16"
  lazy val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  lazy val akkaLogging = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  lazy val akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
  lazy val akkaClusterTyped = "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion
  lazy val akkaClusterShardingTyped =
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion
  lazy val akkaTypedTestkit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion

  lazy val akka =
    Seq(
      akkaActorTyped % Provided,
      akkaClusterTyped % Provided,
      akkaClusterShardingTyped % Provided,
      akkaPersistenceTyped % Provided
    )

  lazy val catsVersion = "2.6.1"
  lazy val cats =
    Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-kernel" % catsVersion
    )

  lazy val catsTaglessVersion = "0.14.0"
  lazy val catsTagless = Seq("org.typelevel" %% "cats-tagless-macros" % catsTaglessVersion)

  lazy val catsEffectVersion = "3.2.8"
  lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % catsEffectVersion)
}
