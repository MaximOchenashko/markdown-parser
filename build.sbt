name := "markdown-parser-service"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging)

lazy val akka = Seq("http-experimental", "slf4j")
  .map(v => "com.typesafe.akka" %% s"akka-$v" % "2.4.8")

lazy val scalaz = Seq("core", "effect")
  .map(v => "org.scalaz" %% s"scalaz-$v" % "7.2.4")

lazy val circe = Seq("core", "generic", "parser", "jawn")
  .map(v => "io.circe" %% s"circe-$v" % "0.4.1")

libraryDependencies ++= Seq(
  "de.heikoseeberger" %% "akka-http-circe" % "1.7.0",
  "com.github.etaty" %% "rediscala" % "1.6.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "org.reactivemongo" %% "reactivemongo" % "0.11.14",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
) ++ akka ++ scalaz ++ circe

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

scriptClasspath := Seq("*")