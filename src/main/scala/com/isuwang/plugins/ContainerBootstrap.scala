package com.isuwang.plugins

import java.lang.reflect.{Field, Method}
import java.net.{URL, URLClassLoader}

import com.isuwang.dapeng.bootstrap.Bootstrap
import com.isuwang.dapeng.bootstrap.classloader._
import com.isuwang.dapeng.container.spring.SpringContainer
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

/**
  * Created by lihuimin on 2017/11/9.
  */
class ContainerBootstrap {

  val logger: Logger = LoggerFactory.getLogger(classOf[ContainerBootstrap])

  val myApp: Seq[URL] = Seq(
    "file:/C:/Users/tangliu/.sbt/boot/scala-2.12.3/lib/scala-library.jar",
    "file:/E:/.m2/repository/com/isuwang/scalatest-api/1.0-SNAPSHOT/scalatest-api-1.0-SNAPSHOT.jar",
    "file:/E:/.m2/repository/com/isuwang/dapeng-remoting-api/1.2.1/dapeng-remoting-api-1.2.1.jar",
    "file:/E:/.m2/repository/com/isuwang/dapeng-core/1.2.1/dapeng-core-1.2.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.slf4j/slf4j-api/jars/slf4j-api-1.7.13.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/io.netty/netty-all/jars/netty-all-4.1.6.Final.jar",
    "file:/E:/.m2/repository/com/isuwang/dapeng-registry-api/1.2.1/dapeng-registry-api-1.2.1.jar",
    "file:/E:/.m2/repository/com/isuwang/dapeng-route-impl/1.2.1/dapeng-route-impl-1.2.1.jar",
    "file:/E:/.m2/repository/com/isuwang/dapeng-route-api/1.2.1/dapeng-route-api-1.2.1.jar",
    "file:/E:/.m2/repository/com/isuwang/dapeng-transaction-api/1.2.1/dapeng-transaction-api-1.2.1.jar",
    "file:/E:/.m2/repository/com/isuwang/dapeng-serializer-api/1.2.1/dapeng-serializer-api-1.2.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.isuwang/dapeng-spring/jars/dapeng-spring-1.2.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.springframework/spring-context/jars/spring-context-4.2.4.RELEASE.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.springframework/spring-aop/jars/spring-aop-4.2.4.RELEASE.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/aopalliance/aopalliance/jars/aopalliance-1.0.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.springframework/spring-beans/jars/spring-beans-4.2.4.RELEASE.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.springframework/spring-core/jars/spring-core-4.2.4.RELEASE.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/commons-logging/commons-logging/jars/commons-logging-1.2.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.springframework/spring-expression/jars/spring-expression-4.2.4.RELEASE.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.apache.commons/commons-lang3/jars/commons-lang3-3.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.isuwang/service-commons_2.11/jars/service-commons_2.11-1.0.5.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.github.wangzaixiang/scala-sql_2.11/jars/scala-sql_2.11-1.0.6.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/redis.clients/jedis/jars/jedis-2.7.2.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.apache.commons/commons-pool2/jars/commons-pool2-2.3.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.google.code.gson/gson/jars/gson-2.7.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.github.wangzaixiang/spray-json_2.11/jars/spray-json_2.11-1.3.4.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.alibaba.otter/canal.protocol/jars/canal.protocol-1.0.22.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.alibaba.otter/canal.common/jars/canal.common-1.0.22.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.apache.zookeeper/zookeeper/jars/zookeeper-3.4.5.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.jboss.netty/netty/bundles/netty-3.2.2.Final.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.github.sgroschupf/zkclient/jars/zkclient-0.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/commons-io/commons-io/jars/commons-io-2.4.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/commons-lang/commons-lang/jars/commons-lang-2.6.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.alibaba/fastjson/jars/fastjson-1.1.35.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.google.guava/guava/bundles/guava-18.0.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.slf4j/jcl-over-slf4j/jars/jcl-over-slf4j-1.7.12.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.google.protobuf/protobuf-java/jars/protobuf-java-2.4.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.springframework/spring-jdbc/jars/spring-jdbc-4.2.4.RELEASE.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/org.springframework/spring-tx/jars/spring-tx-4.2.4.RELEASE.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/log4j/log4j/jars/log4j-1.2.15.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/javax.mail/mail/jars/mail-1.4.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/javax.activation/activation/jars/activation-1.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/javax.jms/jms/jars/jms-1.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/junit/junit/jars/junit-3.8.1.jar",
    "file:/E:/.m2/repository/com/isuwang/dapeng-remoting-netty/1.2.1/dapeng-remoting-netty-1.2.1.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.fasterxml.jackson.core/jackson-databind/bundles/jackson-databind-2.6.2.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.fasterxml.jackson.core/jackson-annotations/bundles/jackson-annotations-2.6.0.jar",
    "file:/C:/Users/tangliu/.ivy2/cache/com.fasterxml.jackson.core/jackson-core/bundles/jackson-core-2.6.2.jar",
    "file:/F:/dapeng/hello-service/target/scala-2.12/hello-service_2.12-0.1-SNAPSHOT.jar"
  ).map(new URL(_))


  def bootstrap(appClasspaths: Seq[URL]): Unit = {
    //    val mainClass = classOf[Bootstrap].getName
    //    val threadGroup = new ThreadGroup(mainClass)
    val myClassLoader = classOf[Bootstrap].getClassLoader
    val containerClassLoader = classOf[SpringContainer].getClassLoader
    //
    //    val bootstrapThread = new Thread(threadGroup,()=>{
    //
    //      try {

    ClassLoaderManager.shareClassLoader = myClassLoader

    ClassLoaderManager.platformClassLoader = myClassLoader

    ClassLoaderManager.appClassLoaders.add(new AppClassLoader(appClasspaths.toArray, null))
    ClassLoaderManager.pluginClassLoaders.add(myClassLoader)

    val springContainerClass = ClassLoaderManager.platformClassLoader.loadClass("com.isuwang.dapeng.container.spring.SpringContainer")
    val appClassLoaderField = springContainerClass.getField("appClassLoaders")
    appClassLoaderField.set(springContainerClass, ClassLoaderManager.appClassLoaders)

    val pluginClassLoaderField = springContainerClass.getField("pluginClassLoaders")
    pluginClassLoaderField.set(springContainerClass, ClassLoaderManager.pluginClassLoaders)

    val mainClass = ClassLoaderManager.platformClassLoader.loadClass("com.isuwang.dapeng.container.ContainerStartup")
    val mainMethod = mainClass.getMethod("startup")
    mainMethod.invoke(mainClass)
    //      }
    //      catch {
    //        case ex: Exception => {
    //          logger.error(ex.getMessage, ex)
    //        }
    //      }
    //    } ,mainClass +".main()")

    //    bootstrapThread.start()

    //    joinNonDaemonThreads(threadGroup)
  }

  def joinNonDaemonThreads(threadGroup: ThreadGroup): Unit = {
    var foundNonDaemon = false
    do {
      foundNonDaemon = false
      val threads = getActiveThreads(threadGroup)

      var hasDeadThread = false
      threads.foreach(thread => {
        if (thread.isDaemon)
          hasDeadThread = true
      })
      if (!hasDeadThread) {
        for (thread <- threads) {
          joinThread(thread, 0)
        }
      }
    } while (foundNonDaemon)
  }

  protected def getActiveThreads(threadGroup: ThreadGroup): List[Thread] = {
    val threads = new Array[Thread](threadGroup.activeCount)
    val result = ListBuffer[Thread]()
    var i = 0
    while ( {
      i < threads.length && threads(i) != null
    }) {
      result.append(threads(i))
      i += 1
    }
    result.toList // note: result should be modifiable
  }

  protected def joinThread(thread: Thread, timeoutMsecs: Long): Unit = {
    try {
      logger.debug("joining on thread " + thread)
      thread.join(timeoutMsecs)
    } catch {
      case e: InterruptedException =>
        Thread.currentThread.interrupt() // good practice if don't throw
        logger.warn("interrupted while joining against thread " + thread, e) // not expected!
    }
    if (thread.isAlive) { // generally abnormal
      logger.warn("thread " + thread + " was interrupted but is still alive after waiting at least " + timeoutMsecs + "msecs")
    }
  }

}
