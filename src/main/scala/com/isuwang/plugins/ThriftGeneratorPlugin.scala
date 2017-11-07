package com.isuwang.plugins

import com.isuwang.dapeng.code.Scrooge
import sbt.AutoPlugin
import sbt.Keys._
import sbt._
import scala.io._

// ApiPlugin: idlc
// ServicePlugin: dp-dist, dp-docker, dp-run
object ThriftGeneratorPlugin extends AutoPlugin{


  val generateFiles = taskKey[Seq[java.io.File]]("generate thrift file sources")

  def generateFilesTask = Def.task {
    lazy val sourceFilesPath = (baseDirectory in Compile).value.getAbsolutePath + "/src/main/resources/thrifts"
    lazy val targetFilePath =  (baseDirectory in Compile).value.getAbsolutePath + "/src/main"


    generateFiles(sourceFilesPath,targetFilePath)

    Seq[java.io.File]()
  }

  override lazy val projectSettings = inConfig(Compile)(Seq(
    generateFiles := generateFilesTask.value,
    sourceGenerators += generateFiles.taskValue
  ))

  def generateFiles(sourceFilePath: String, targetFilePath: String) = {

    val javaFilePath = targetFilePath + "/java"
    if (!file(javaFilePath).exists()) {
      file(targetFilePath).mkdir()
    }
    println(s"sourceFilePath: ${sourceFilePath}, targetFilePath: ${javaFilePath}")
    Scrooge.main(Array("-gen", "java", "-all",
      "-in", sourceFilePath,
      "-out", javaFilePath))

    val scalaFilePath = targetFilePath + "/scala"
    if (!file(scalaFilePath).exists()) {
      file(scalaFilePath).mkdir()
    }
    println(s" sourceFilePath: ${sourceFilePath}, targetFilePath: ${scalaFilePath}")
    Scrooge.main(Array("-gen", "scala", "-all",
      "-in", sourceFilePath,
      "-out", scalaFilePath))

  }

}
