package com.isuwang.plugins

import java.net.URL

import com.isuwang.dapeng.bootstrap.Bootstrap
import com.isuwang.dapeng.bootstrap.classloader._
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by lihuimin on 2017/11/9.
  */
class ContainerBootstrap {

  val logger: Logger = LoggerFactory.getLogger(classOf[ContainerBootstrap])

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

}

