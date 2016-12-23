name := """scala-bootstrap"""

val commonSettings = Seq(
  organization := "com.chakming",
  scalaVersion := "2.11.8"
)

import com.typesafe.config.{Config, ConfigFactory}

lazy val typesafeConfig = settingKey[Config]("Config from Typesafe config")
typesafeConfig := (resourceDirectory in Compile)
  .apply(_ / "sbt.conf")
  .apply(ConfigFactory.parseFile(_).resolve())
  .value

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(commonSettings ++ {
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-unchecked",
      "-feature",
      "-language:postfixOps",
      "-Xfatal-warnings",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture",
      "-Ywarn-unused-import"
    )
  })
  .settings(
    name := "op-rabbit-stream",
    publishArtifact := false,
    resolvers ++= Seq(
      "SpinGo OSS" at "http://spingo-oss.s3.amazonaws.com/repositories/releases"
    ),
    libraryDependencies ++= {
      lazy val miscDep = List(
        "com.typesafe" % "config" % "1.3.1",
        "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
        "org.typelevel" %% "cats" % "0.8.1",
        "com.softwaremill.quicklens" %% "quicklens" % "1.4.8"
      )
      lazy val loggingDep = List(
        "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
        "ch.qos.logback" % "logback-classic" % "1.1.8"
      )
      lazy val diDep = List(
        "com.softwaremill.macwire" %% "util" % "2.2.5",
        "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
      )
      lazy val rabbitDep = List(
        "com.spingo" %% "op-rabbit-core" % "1.6.0",
        "com.spingo" %% "op-rabbit-akka-stream" % "1.6.0"
      )
      lazy val akkaDep = List(
        "com.typesafe.akka" % "akka-actor_2.11" % "2.4.14",
        "com.typesafe.akka" % "akka-stream_2.11" % "2.4.14"
      )
      lazy val testDep = List(
        "org.scalatest" %% "scalatest" % "3.0.1",
        "org.mockito" % "mockito-core" % "2.4.0"
      ) map (_ % "it,test")

      miscDep ::: loggingDep ::: diDep ::: rabbitDep ::: akkaDep ::: testDep
    }
  )
  .enablePlugins(GitVersioning, JavaAppPackaging)
