package com.isuwang.plugins

import java.net.URL

import com.isuwang.dapeng.container.ContainerStartup
import sbt.Keys._
import sbt.{AutoPlugin, _}
import _root_.io.netty.buffer.ByteBuf
import com.isuwang.dapeng.doc.ApiServiceController
import com.isuwang.dapeng.message.consumer.kafka.KafkaConsumer
import com.isuwang.dapeng.monitor.api.MonitorServiceClient
import com.isuwang.dapeng.registry.zookeeper.RegistryAgentImpl
import com.isuwang.dapeng.remoting.netty.SoaClient
import com.isuwang.dapeng.transaction.GlobalTransactionManager
import org.codehaus.commons.compiler.IScriptEvaluator
import org.quartz.Calendar
import org.slf4j.{Logger, LoggerFactory}
/**
  * Created by lihuimin on 2017/11/8.
  */
object RunContainerPlugin  extends AutoPlugin {

  val runContainer = taskKey[Unit]("run dapeng container")
  val logger = LoggerFactory.getLogger(getClass)


  def runDapeng(projectPath:String,appClasspaths:Seq[URL]): Unit ={
    System.setProperty("soa.base", projectPath)
    System.setProperty("soa.run.mode", "maven")
    System.setProperty("soa.apidoc.port","8192")
    System.setProperty("soa.zookeeper.kafka.host","192.168.99.100:2181")
    System.setProperty("soa.zookeeper.host","192.168.99.100:2181")
    System.setProperty("soa.transactional.enable","false")
    System.setProperty("soa.monitor.enable","false")
    System.setProperty("soa.container.port","9095")
    new ContainerBootstrap().bootstrap(appClasspaths)
  }


  override lazy val projectSettings=Seq(
      runContainer := {
        logger.info("starting dapeng container....")
        val dependentClasspaths=(managedClasspath in Compile).value.map(
          _.data.toURI.toURL
        )

        val projectPath = (baseDirectory in Compile).value.getAbsolutePath
        val serviceJarURL=(packageBin in Compile).value.toURI.toURL
        val classpathsWithDapeng=serviceJarURL::dependentClasspaths.toList
        runDapeng(projectPath,classpathsWithDapeng)
      },

      logLevel in runContainer := Level.Info
  )



}

