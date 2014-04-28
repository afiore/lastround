import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.lastroundapp",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.10.3",
    scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature", "-encoding", "utf8"),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )
}

object Dependencies {
  val akkaVersion  = "2.2.3"
  val sprayVersion = "1.2.0"
  val sprayCan     = "io.spray" % "spray-can" % sprayVersion
  val sprayClient  = "io.spray" % "spray-client" % sprayVersion
  val sprayRouting = "io.spray" % "spray-routing" % sprayVersion
  val sprayJson    = "io.spray" %% "spray-json" % "1.2.5"
  val sprayTest    = "io.spray" % "spray-testkit" % sprayVersion % "test"
  val akkaActor    = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaSlf4j    = "com.typesafe.akka"   %%  "akka-slf4j" % akkaVersion
  val akkaTestkit  = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
  val scalaLogging = "com.typesafe" %% "scalalogging-slf4j" % "1.0.1"
  val logback      = "ch.qos.logback" % "logback-classic" % "1.0.13"
  val specs2       = "org.specs2" %% "specs2" % "2.3.11" % "test"
  val mockito      = "org.mockito" % "mockito-core" % "1.9.5" % "test"
  val commonDeps   = Seq(
    sprayCan,
    sprayClient,
    sprayRouting,
    sprayJson,
    sprayTest,
    akkaActor,
    akkaSlf4j,
    akkaTestkit,
    scalaLogging,
    logback,
    specs2,
    mockito
  )
}

object Resolvers {
  val sprayRepo = "spray repo" at "http://repo.spray.io/"
  val resolvers = Seq(sprayRepo)
}

object LastroundauthBuild extends Build {

  import BuildSettings.buildSettings
  import Dependencies.commonDeps
  import spray.revolver.RevolverPlugin.Revolver
  import org.scalastyle.sbt.ScalastylePlugin.{Settings => ScalastyleSettings}
  import sbtassembly.Plugin.AssemblyKeys
  import sbtassembly.Plugin.assemblySettings

  lazy val root = Project("lastround-auth",
    file("."),
    settings = buildSettings ++
      ScalastyleSettings ++
      assemblySettings ++
      Revolver.settings) settings (
    libraryDependencies ++= commonDeps,
        resolvers := Resolvers.resolvers,
        mainClass in AssemblyKeys.assembly := Some("com.lastroundapp.Boot")
    )
}
