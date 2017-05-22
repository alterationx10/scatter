name := """scatter"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.amazonaws" % "aws-java-sdk" % "1.11.132",
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0",
  "org.postgresql" % "postgresql" % "42.1.1",
  "com.github.tminglei" %% "slick-pg" % "0.15.0-RC"
)

