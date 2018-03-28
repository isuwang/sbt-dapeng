
name := "sbt-dapeng"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.2"

sbtPlugin := true

organization := "com.isuwang"

resolvers += Resolver.mavenLocal

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")


libraryDependencies ++= Seq(
  "com.github.dapeng" % "dapeng-code-generator_2.12" % "2.0.0-SNAPSHOT" exclude("javax.servlet", "servlet-api"),
  "com.github.dapeng" % "dapeng-container-impl"% "2.0.0-SNAPSHOT",
  "com.github.dapeng" % "dapeng-bootstrap" % "2.0.0-SNAPSHOT",
  "com.github.dapeng" % "dapeng-client-netty"% "2.0.0-SNAPSHOT"
)

resolvers ++= List("isuwang nexus" at "http://nexus.oa.isuwang.com/repository/maven-public")

publishTo := Some("nexus-releases" at "http://nexus.oa.isuwang.com/repository/maven-releases/")

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.oa.isuwang.com", "admin", "6d17f21ed")

