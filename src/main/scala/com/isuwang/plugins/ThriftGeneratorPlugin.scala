package com.isuwang.plugins

import java.io.File

import com.github.dapeng.code.Scrooge
import sbt.AutoPlugin
import sbt.Keys._
import sbt._

import scala.io._

// ApiPlugin: idlc
// ServicePlugin: dp-dist, dp-docker, dp-run
object ThriftGeneratorPlugin extends AutoPlugin{


  val generateFiles = taskKey[Seq[java.io.File]]("generate thrift file sources")

  def generateFilesTask = Def.task {
    lazy val sourceFilesPath = (baseDirectory in Compile).value.getAbsolutePath + "/src/main/resources"
    lazy val targetFilePath =  (baseDirectory in Compile).value.getAbsolutePath + "/target/scala-2.12/src-generated"

    generateFiles(sourceFilesPath,targetFilePath)
  }

  def generateResourceFileTask = Def.task {
    lazy val targetFilePath =  (baseDirectory in Compile).value.getAbsolutePath + "/target/scala-2.12/src-generated/resources"
    getFiles(targetFilePath)
  }

  override lazy val projectSettings = inConfig(Compile)(Seq(
    generateFiles := generateFilesTask.value,
    sourceGenerators += generateFiles.taskValue,
    resourceGenerators += generateResourceFileTask.taskValue
  ))

  def generateFiles(sourceFilePath: String, targetFilePath: String) = {

    println("Welcome to use generate plugin...")
    val javaTargetPath = targetFilePath + "/java"
    createForlder(javaTargetPath)
    println(s" Java files target path: ${javaTargetPath}")

    Scrooge.main(Array("-gen", "java", "-all",
      "-in", sourceFilePath,
      "-out", targetFilePath))

    val javaFiles: Seq[File] = getFiles(javaTargetPath)
    javaFiles.foreach(i => println(s" javaFile: ${i.getAbsolutePath}"))

    val scalaTargetPath = targetFilePath + "/scala"
    createForlder(scalaTargetPath)
    println(s" scala files target path: ${scalaTargetPath}")

    Scrooge.main(Array("-gen", "scala", "-all",
      "-in", sourceFilePath,
      "-out", targetFilePath))

    val scalaFiles: Seq[File] = getFiles(scalaTargetPath)
    scalaFiles.foreach(i => println(s" scalaFiles: ${i.getAbsolutePath}"))

    javaFiles ++ scalaFiles

  }

  def createForlder(path: String) = {
    println(s" start to check whether the folder exists...${path}")
    val paths = path.split("/")

    var folder = "";

    paths.foreach(p => {
      folder += "/" + p
      println(s" check folder: ${folder}")
      val file = new File(folder)
      if (!file.exists()) {
        println(s" Folder Not found. create new one...${file.getAbsoluteFile}")
        file.mkdir();
      }
    })
  }


  def main(args: Array[String]): Unit = {
    val path = "/Users/jackliang/dev/github/sbt-dapeng" + "/target/scala-2.12/src-generated"

    val file = new File(path);
    println(s" folder exists: ${file.exists()}, isFolder: ${file.isDirectory}")

    createForlder(path);

    val file1 = new File(path);

    println(s" folder exists: ${file1.exists()}, isFolder: ${file1.isDirectory}")
  }

  def getFiles(path: String): List[File] = {
    if (!new File(path).isDirectory) {
      List(new File(path))
    } else {
      new File(path).listFiles().flatMap(i => getFiles(i.getPath)).toList
    }
  }

}
