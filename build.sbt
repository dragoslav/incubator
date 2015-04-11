organization in ThisBuild := "nl.proja"

name := """incubator"""

description in ThisBuild := """DIY Incubator"""

version in ThisBuild := "0.1.0"

scalaVersion := "2.11.6"

scalaVersion in ThisBuild := scalaVersion.value

publishMavenStyle := true

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-encoding", "UTF-8")

resolvers in ThisBuild ++= Seq(
  "Spray repo" at "http://repo.spray.io/",
  "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Maven Repository" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.mavenLocal
)

dependencyOverrides in ThisBuild ++= Set(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-library" % scalaVersion.value
)

scalacOptions += "-target:jvm-1.8"

javacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies in ThisBuild ++= Seq(
  "nl.proja" %% "common" % "0.1.0",
  "org.elasticsearch" % "elasticsearch" % "1.5.0",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.akka" %% "akka-remote" % "2.3.9",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.9",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe" % "config" % "1.3.0-M1"
)

lazy val root = project.in(file(".")).settings(
  packagedArtifacts in file(".") := Map.empty,
  run := {
    (run in assembly in Compile).evaluated
  }
).aggregate(
    core, assembly
  ).disablePlugins(sbtassembly.AssemblyPlugin)

lazy val core = project.settings(
  libraryDependencies ++= Seq(
  )
)

lazy val assembly = project.settings(
  libraryDependencies ++= Seq(
    "nl.proja" %% "server" % "0.1.0",
    "com.pi4j" % "pi4j-core" % "1.0-SNAPSHOT"
  )
).dependsOn(core)

spray.revolver.RevolverPlugin.Revolver.settings.settings
