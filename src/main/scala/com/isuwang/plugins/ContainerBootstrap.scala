package com.isuwang.plugins

import java.lang.reflect.{Field, Method}
import java.net.URL
import java.util

import com.isuwang.dapeng.impl.Bootstrap
import org.slf4j.{Logger, LoggerFactory}
import collection.JavaConverters._


/**
  * Created by lihuimin on 2017/11/9.
  */
class ContainerBootstrap {

  val logger: Logger = LoggerFactory.getLogger(classOf[ContainerBootstrap])
  var lastCompiledTime: Long = 0l


  def bootstrap(appClasspaths: Seq[URL]): Unit = {
    try {
      val platformLoader = classOf[Bootstrap].getClassLoader
      Thread.currentThread().setContextClassLoader(platformLoader)

      val bootStrap = platformLoader.loadClass("com.isuwang.dapeng.impl.Bootstrap");
      val field = bootStrap.getField("appURLs")
      val appPaths = new util.ArrayList[java.util.List[URL]]();
      appPaths.add(appClasspaths.asJava);
      field.set(bootStrap, appPaths);

      val method = bootStrap.getMethod("main", classOf[Array[String]])
      method.invoke(null, null);

    }
    catch {
      case ex: Exception => {
        println(ex.getStackTrace)
        logger.error(ex.getMessage, ex)
      }
    }


  }



}

