package com.isuwang.plugins

import java.net.URL

import com.isuwang.dapeng.bootstrap.Bootstrap
import com.isuwang.dapeng.bootstrap.classloader._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

/**
  * Created by lihuimin on 2017/11/9.
  */
class ContainerBootstrap {

  val logger: Logger = LoggerFactory.getLogger(classOf[ContainerBootstrap])

  def bootstrap(appClasspaths: Seq[URL]): Unit = {
    val bootstrapClass = classOf[Bootstrap].getName
    val threadGroup = new ThreadGroup(bootstrapClass)
    val myClassLoader = classOf[Bootstrap].getClassLoader

//    val bootstrapThread = new Thread(threadGroup, () => {

      try {

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
      }
      catch {
        case ex: Exception => {
          logger.error(ex.getMessage, ex)
        }
      }
//    }, bootstrapClass + ".main()")
//
//    bootstrapThread.start()
//
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
    result.toList
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
