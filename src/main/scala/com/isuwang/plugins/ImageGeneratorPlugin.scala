package com.isuwang.plugins

import sbt._
import sbtdocker.DockerKeys.{docker, imageNames}
import sbt.Keys._
import sbtdocker.ImageName
import sbtdocker.DockerPlugin.autoImport.dockerfile
import com.typesafe.sbt.GitPlugin.autoImport._

/**
  * Created by lihuimin on 2017/11/7.
  */
object ImageGeneratorPlugin extends AutoPlugin {

  override def requires = sbtdocker.DockerPlugin&&com.typesafe.sbt.GitPlugin

  override lazy val projectSettings = Seq(
    dockerfile in docker := {
      // any vals to be declared here
      new sbtdocker.mutable.Dockerfile {
        from("docker.oa.isuwang.com:5000/system/dapeng-container:1.2.1")

        val containerHome = "/dapeng-container"
        run("mkdir", "-p", containerHome)

        lazy val sourceFilesPath = (baseDirectory in Compile).value.getAbsolutePath + System.getProperty("file.separator") +"docker"+ System.getProperty("file.separator") + "startup.sh"
        lazy val startupFile = new File(sourceFilesPath)

        copy((packageBin in Compile).value, containerHome + "/apps")
        copy(startupFile, containerHome + "/bin/")
        run("chmod", "+x", containerHome + "/bin/startup.sh")
        workDir(containerHome + "/bin")

        cmd("/bin/sh", "-c", containerHome + "/bin/startup.sh && tail -F " + containerHome + "/bin/startup.sh")
      }
    },

    imageNames in docker := Seq (
      ImageName(
        namespace = Some("docker.oa.isuwang.com:5000/product"),
        repository = name.value,
        tag = Some(git.gitHeadCommit.value match { case Some(tag) => tag.substring(0, 7) case None => "latest" })
      )
    )
  )




}

