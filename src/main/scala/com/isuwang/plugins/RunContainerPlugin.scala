package com.isuwang.plugins

import java.net.URL

import org.slf4j.LoggerFactory
import sbt.Keys._
import sbt.{AutoPlugin, _}
/**
  * Created by lihuimin on 2017/11/8.
  */
object RunContainerPlugin  extends AutoPlugin  {

  val runContainer = taskKey[Unit]("run dapeng container")
  val logger = LoggerFactory.getLogger(getClass)


  def runDapeng(projectPath:String,appClasspaths:Seq[URL]) : Unit ={
    System.setProperty("soa.base", projectPath)
    System.setProperty("soa.run.mode", "maven")
    System.setProperty("soa.apidoc.port","8192")
    System.setProperty("soa.zookeeper.kafka.host","127.0.0.1:2181")
    System.setProperty("soa.zookeeper.host","127.0.0.1:2181")
    System.setProperty("soa.transactional.enable","false")
    System.setProperty("soa.monitor.enable","false")
    System.setProperty("soa.container.port","9095")
    val bootstrapThread = new Thread( () => {
      new ContainerBootstrap().bootstrap(appClasspaths)
    })
    bootstrapThread.start()
    bootstrapThread.join()
  }


  override lazy val projectSettings=Seq(
      runContainer := {
        logger.info("starting dapeng container....")
        val dependentClasspaths=(fullClasspath in Compile).value.map(
          _.data.toURI.toURL
        )

        val projectPath = (baseDirectory in Compile).value.getAbsolutePath
        val classpathsWithDapeng=dependentClasspaths.toList
        runDapeng(projectPath,classpathsWithDapeng)
      },

      logLevel in runContainer := Level.Info
  )



}

