import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.lastroundapp",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.10.4",
    scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-feature", "-encoding", "utf8"),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )
}

object Dependencies {
  val akkaVersion     = "2.3.2"
  val sprayVersion    = "1.3.2-20140428"
  val sprayCan        = "io.spray" % "spray-can" % sprayVersion
  val sprayClient     = "io.spray" % "spray-client" % sprayVersion
  val sprayRouting    = "io.spray" % "spray-routing" % sprayVersion
  val sprayJson       = "io.spray" %% "spray-json" % "1.2.6"
  val sprayTest       = "io.spray" % "spray-testkit" % sprayVersion % "test"
  val akkaActor       = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaSlf4j       = "com.typesafe.akka"   %%  "akka-slf4j" % akkaVersion
  val akkaTestkit     = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
  val scalaLogging    = "com.typesafe" %% "scalalogging-slf4j" % "1.0.1"
  val logback         = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val scalaTest       = "org.scalatest" % "scalatest_2.10" % "2.1.4" % "test"
  val jodaTime        = "joda-time" % "joda-time" % "2.0"
  val jodaTimeConvert = "org.joda" % "joda-convert" % "1.2"
  val commonDeps      = Seq(
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
    jodaTime,
    jodaTimeConvert,
    scalaTest
  )
}

object Resolvers {
  val sprayRepo        = "spray repo" at "http://repo.spray.io/"
  val resolvers        = Seq(sprayRepo)
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
