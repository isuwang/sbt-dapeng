package com.isuwang.plugins

import java.io.File
import java.net.URL

import com.isuwang.dapeng.bootstrap.Bootstrap
import com.isuwang.dapeng.bootstrap.classloader._
import com.isuwang.dapeng.container.registry.ProcessorCache
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.JavaConverters._

/**
  * Created by lihuimin on 2017/11/9.
  */
class ContainerBootstrap {

  val logger: Logger = LoggerFactory.getLogger(classOf[ContainerBootstrap])
  var lastCompiledTime: Long = 0l

  def bootstrap(appClasspaths: Seq[URL]): Unit = {
    try {
      val myClassLoader = classOf[Bootstrap].getClassLoader

        ClassLoaderManager.shareClassLoader = myClassLoader

        ClassLoaderManager.platformClassLoader = myClassLoader

        ClassLoaderManager.appClassLoaders.add(new AppClassLoader(appClasspaths.toArray, null))
        ClassLoaderManager.pluginClassLoaders.add(myClassLoader)

        val springContainerClass = ClassLoaderManager.platformClassLoader.loadClass("com.isuwang.dapeng.container.spring.SpringContainer")
        val appClassLoaderField = springContainerClass.getField("appClassLoaders")
        appClassLoaderField.set(springContainerClass, ClassLoaderManager.appClassLoaders)

        val pluginClassLoaderField = springContainerClass.getField("pluginClassLoaders")
        pluginClassLoaderField.set(springContainerClass, ClassLoaderManager.pluginClassLoaders)

        val old = Thread.currentThread().getContextClassLoader
        try {
          compileCheck(appClasspaths, appClasspaths(0).getPath)

          Thread.currentThread().setContextClassLoader(ClassLoaderManager.platformClassLoader)

          val mainClass = ClassLoaderManager.platformClassLoader.loadClass("com.isuwang.dapeng.container.ContainerStartup")
          val mainMethod = mainClass.getMethod("startup")
          mainMethod.invoke(mainClass)

        }
        finally {
          Thread.currentThread().setContextClassLoader(old);
        }
      }
      catch {
        case ex: Exception => {
          logger.error(ex.getMessage, ex)
        }
      }


  }


  def reboot(appClasspaths: Seq[URL]) = {

    println(" Start to reboot springContainer......")

    ClassLoaderManager.appClassLoaders.clear()

    ClassLoaderManager.appClassLoaders.add(new AppClassLoader(appClasspaths.toArray, null))

    System.gc() // 并不一定有用，只是告诉jvm gc一下

    println(s" current appClassLoader hashCode: ${ClassLoaderManager.appClassLoaders.get(0).hashCode()}")

    //restart springContainer
    val springContainerClass = ClassLoaderManager.platformClassLoader.loadClass("com.isuwang.dapeng.container.spring.SpringContainer")

    val appClassLoaderField = springContainerClass.getField("appClassLoaders")
    appClassLoaderField.set(springContainerClass, ClassLoaderManager.appClassLoaders)

    val springContainerInstance = springContainerClass.newInstance()
    val startup = springContainerClass.getMethod("start")
    startup.invoke(springContainerInstance)

    //TODO: TO be enhanced...
    //zookeeperContainer & LocalRegistryContainer
    //只是修改ProcessorCache & SoaAppClassLoaderCache 是否需要重启
    loadContainerClassToStart("com.isuwang.dapeng.container.registry.ZookeeperRegistryContainer")

    //ScheduledTaskContainer
    loadContainerClassToStart("com.isuwang.dapeng.container.timer.ScheduledTaskContainer")

    //NettyContainer
    val nettyThread = Thread.getAllStackTraces.keySet()
      .asScala.toList.find(i => i.getName.equals("NettyContainer-Thread"))
    nettyThread match {
      case Some(t) =>
        println(" found netty container thread........")
        try {
          t.interrupt()
        } catch {
          case e: InterruptedException => println(s" failed to interrupted thread..${e.getMessage}")
          case e1: Exception => println(s" failed to intterupted e1....${e1.getMessage}")
          case _ => println(s" has interrupted: ${t.isInterrupted}, isAlive: ${t.isAlive}")
        } finally {
          println(s" finally has interrupted: ${t.isInterrupted}, isAlive: ${t.isAlive}")
        }

        var interrupted = false
        while (!interrupted) {
            if (!t.isInterrupted && t.isAlive) {
              println(s" nettyContainer has not interrupted.sleeping 2000ms..isInterrupted: ${t.isInterrupted}.  isAlive: ${t.isAlive}")
              Thread.sleep(2000)
            } else {
              interrupted = true
              println(" Start to load nettyContainer......")
              //TODO: 问题， 如何获取nettyContainer bootstrap的实例，更新childHandler
              loadContainerClassToStart("com.isuwang.dapeng.container.netty.NettyContainer")
            }
        }
      case _ =>
        println(" Can't find netty thread NettyContainer-Thread. can start to reboot nettyContainer.....")
        loadContainerClassToStart("com.isuwang.dapeng.container.netty.NettyContainer")

    }

  }

  def loadContainerClassToStart(className: String): Unit = {
    val containerClass = ClassLoaderManager.platformClassLoader.loadClass(className)
    val startup = containerClass.getMethod("start")
    startup.invoke(containerClass.newInstance())
  }


  def compileCheck(appClasspaths: Seq[URL], baseDir: String): Unit = {
    val compileCheck = new Thread(() => {
      val targetDir = new File(s"${baseDir}").getParentFile
      lastCompiledTime = targetDir.lastModified()
      println(" start to check if has new compilation..")
      while (true) {

        println(s"while looping.. lastCompileTime: ${lastCompiledTime}, ${targetDir.getAbsolutePath} , compileTime: ${targetDir.lastModified()}.....")
        if (lastCompiledTime < targetDir.lastModified()) {
          lastCompiledTime = targetDir.lastModified()
          println(" Found new compilation....start to reboot....")
          reboot(appClasspaths)
        } else {
          Thread.sleep(5000)
        }
      }
    })
    compileCheck.start()
    //compileCheck.join()
  }

}

