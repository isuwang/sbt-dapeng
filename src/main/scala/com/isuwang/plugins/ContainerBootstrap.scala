package com.isuwang.plugins

import java.net.{URL, URLClassLoader}

import com.isuwang.dapeng.bootstrap.Bootstrap
import com.isuwang.dapeng.bootstrap.classloader._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

/**
  * Created by lihuimin on 2017/11/9.
  */
class ContainerBootstrap {

  val logger: Logger = LoggerFactory.getLogger(classOf[ContainerBootstrap])

  def bootstrap(classpaths:Seq[URL],serviceJarName:String): Unit ={
    val mainClass = classOf[Bootstrap].getName
    val threadGroup = new ThreadGroup(mainClass)
    val bootstrapThread = new Thread(threadGroup,()=>{

      try {
        val urls: Array[URL] = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs
        val shareUrls=urls.filterNot(_.getFile.matches("^.*/(dapeng-transaction-impl|dapeng-container|dapeng-bootstrap|"+serviceJarName+").*\\.jar$"))
        val appUrls = urls.filterNot(_.getFile.matches("^.*/(dapeng-container|dapeng-bootstrap).*\\.jar$"))
        val platformUrls = urls.filterNot(_.getFile.matches("^.*/"+serviceJarName+".*\\.jar$"))

        ClassLoaderManager.shareClassLoader = new ShareClassLoader(shareUrls)
        ClassLoaderManager.platformClassLoader=new PlatformClassLoader(platformUrls)
        ClassLoaderManager.appClassLoaders.add(new AppClassLoader(appUrls))
        ClassLoaderManager.pluginClassLoaders.add(new PluginClassLoader(shareUrls))

        Bootstrap.main(new Array[String](0))
      }
      catch {
        case ex: Exception => {
          logger.error(ex.getMessage, ex)
        }
      }
    } ,mainClass +".main()")

    bootstrapThread.setContextClassLoader(new URLClassLoader(classpaths.toArray))
    bootstrapThread.start()

    joinNonDaemonThreads(threadGroup)
  }

  def joinNonDaemonThreads(threadGroup: ThreadGroup): Unit = {
    var foundNonDaemon = false
    do {
      foundNonDaemon = false
      val threads = getActiveThreads(threadGroup)

      var hasDeadThread=false
      threads.foreach(thread =>{
        if(thread.isDaemon)
          hasDeadThread=true
      })
      if(!hasDeadThread){
        for (thread <- threads){
          joinThread(thread, 0)
        }
      }
    } while (foundNonDaemon)
  }

  protected def getActiveThreads(threadGroup: ThreadGroup): List[Thread] = {
    val threads = new Array[Thread](threadGroup.activeCount)
    val result = ListBuffer[Thread]()
    var i = 0
    while ( {i < threads.length && threads(i) != null}) {
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
