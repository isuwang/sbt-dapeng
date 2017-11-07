name := "sbt-idlc"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.2"

sbtPlugin := true

organization := "com.isuwang"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.isuwang" %% "dapeng-code-generator" % "1.2.1"
)