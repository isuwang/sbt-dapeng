package com.isuwang.plugins

import java.net.URL

import _root_.io.netty.buffer.ByteBuf
import com.isuwang.dapeng.container.ContainerStartup
import com.isuwang.dapeng.doc.ApiServiceController
import com.isuwang.dapeng.message.consumer.kafka.KafkaConsumer
import com.isuwang.dapeng.monitor.api.MonitorServiceClient
import com.isuwang.dapeng.registry.zookeeper.RegistryAgentImpl
import com.isuwang.dapeng.remoting.netty.SoaClient
import com.isuwang.dapeng.transaction.GlobalTransactionManager
import org.codehaus.commons.compiler.IScriptEvaluator
import org.quartz.Calendar
import sbt.Keys._
import sbt.{AutoPlugin, _}

/**
  * Created by lihuimin on 2017/11/8.
  */
object RunContainerPlugin  extends AutoPlugin{

  val runContainer = taskKey[Unit]("run dapeng container")


  def runDapeng(projectPath:String,classpaths:Seq[URL],serviceJarName:String): Unit ={
    System.setProperty("soa.base", projectPath)
    System.setProperty("soa.run.mode", "sbt")
    System.setProperty("soa.apidoc.port","8192")
    System.setProperty("soa.zookeeper.kafka.host","192.168.99.100:2181")
    System.setProperty("soa.zookeeper.host","192.168.99.100:2181")
    System.setProperty("soa.run.mode","native")
    System.setProperty("soa.transactional.enable","false")
    System.setProperty("soa.monitor.enable","false")
    System.setProperty("soa.container.port","9095")
    new ContainerBootstrap().bootstrap(classpaths,serviceJarName)
  }


  override lazy val projectSettings=Seq(
    {
     runContainer := {
       val classpaths=(dependencyClasspath in Compile).value.map(
         _.data.toURI.toURL
       )
       val classpathsWithDapeng= getPathOfDapengContainer() :::classpaths.toList

       val projectPath = (baseDirectory in Compile).value.getAbsolutePath
       val serviceJarName=(packageBin in Compile).value.getName
       runDapeng(projectPath,classpathsWithDapeng,serviceJarName)
     }
    }
  )



  def getPathOfDapengContainer(): List[URL] ={
    val dapengContainerJar = classOf[ContainerStartup].getProtectionDomain.getCodeSource.getLocation
    val logbackCoreJar = classOf[ch.qos.logback.core.Context].getProtectionDomain.getCodeSource.getLocation
    val logbackClassicJar = classOf[ch.qos.logback.classic.Logger].getProtectionDomain.getCodeSource.getLocation
    val slf4jJar =  classOf[org.slf4j.Logger].getProtectionDomain.getCodeSource.getLocation
    val codehausJar =  classOf[org.codehaus.janino.Access].getProtectionDomain.getCodeSource.getLocation
    val codehausCommonJar = classOf[IScriptEvaluator].getProtectionDomain.getCodeSource.getLocation
    val nettyJar = classOf[ByteBuf].getProtectionDomain.getCodeSource.getLocation
    val registryZookeeperJar =classOf[RegistryAgentImpl].getProtectionDomain.getCodeSource.getLocation
    val apiDocJar = classOf[ApiServiceController].getProtectionDomain.getCodeSource.getLocation
    val monitorJar =classOf[MonitorServiceClient].getProtectionDomain.getCodeSource.getLocation
    val remotingNettyJar = classOf[SoaClient].getProtectionDomain.getCodeSource.getLocation
    val transactionJar = classOf[GlobalTransactionManager].getProtectionDomain.getCodeSource.getLocation
    val quartz=classOf[Calendar].getProtectionDomain.getCodeSource.getLocation
    val kafkaJar = classOf[KafkaConsumer].getProtectionDomain.getCodeSource.getLocation

    List(dapengContainerJar,logbackCoreJar,logbackClassicJar,slf4jJar,codehausJar,codehausCommonJar,nettyJar,registryZookeeperJar,apiDocJar,monitorJar,remotingNettyJar,transactionJar,quartz,kafkaJar)
  }


}

