organization := "nl.proja"


name := """PiStraw"""

version := "0.1.0"

scalaVersion := "2.11.5"

description := """PiStraw"""


resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.mavenLocal
)

libraryDependencies ++= Seq(
  "nl.proja" %% "common" % "0.1.0",
  "org.elasticsearch" % "elasticsearch" % "1.5.0",
  "com.sksamuel.elastic4s" %% "elastic4s" % "1.5.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.0",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.5.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.akka" %% "akka-remote" % "2.3.9",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.9",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe" % "config" % "1.3.0-M1",
  "junit" % "junit" % "4.11" % "test"
)



