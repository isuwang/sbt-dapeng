package com.isuwang.plugins

import java.io.FileInputStream
import java.net.URL
import java.util.Properties

import org.slf4j.LoggerFactory
import sbt.Keys._
import sbt.{AutoPlugin, _}
import collection.JavaConversions._
/**
  * Created by lihuimin on 2017/11/8.
  */
object RunContainerPlugin  extends AutoPlugin  {

  val runContainer = taskKey[Unit]("run dapeng container")
  val logger = LoggerFactory.getLogger(getClass)


  def runDapeng(appClasspaths:Seq[URL]) : Unit ={
    val bootstrapThread = new Thread( () => {
      new ContainerBootstrap().bootstrap(appClasspaths)
    })
    bootstrapThread.start()
    bootstrapThread.join()
  }

  def loadSystemProperties(file :File):Unit = {
    val properties = new Properties()
    properties.load(new FileInputStream(file))
    val results = properties.keySet().map(_.toString)

    results.foreach(keyString=>{
      System.setProperty(keyString,properties.getProperty(keyString))
    })
  }

  override lazy val projectSettings=Seq(
      runContainer := {
        logger.info("starting dapeng container....")
        val projectPath = (baseDirectory in Compile).value.getAbsolutePath
        System.setProperty("soa.base", projectPath)
        var propertiesFile :File=null
        (unmanagedResources in Compile).value.foreach(resource=>{
          if(resource.getName.equals("dapeng.properties")){
            propertiesFile = resource
          }
        })
        if(propertiesFile!=null){
          loadSystemProperties(propertiesFile)
        }
        val dependentClasspaths=(fullClasspath in Compile).value.map(
          _.data.toURI.toURL
        )
        val classpathsWithDapeng=dependentClasspaths.toList
        runDapeng(classpathsWithDapeng)
      },

      logLevel in runContainer := Level.Info
  )



}

