import sbt.internal.util.complete.Parser

name := "sbt-idlc"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.2"

sbtPlugin := true

organization := "com.isuwang"

resolvers += Resolver.mavenLocal

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")


libraryDependencies ++= Seq(
  "com.isuwang" %% "dapeng-code-generator" % "1.2.1" exclude("javax.servlet", "servlet-api"),
  "com.isuwang" % "dapeng-bootstrap"% "1.2.1"
)