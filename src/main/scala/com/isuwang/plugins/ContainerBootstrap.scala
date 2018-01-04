package com.isuwang.plugins

import java.net.{URL, URLClassLoader}
import java.util

import com.isuwang.dapeng.bootstrap.Bootstrap
import com.isuwang.dapeng.bootstrap.classloader.ApplicationClassLoader
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
//      val appPaths = new util.ArrayList[java.util.List[URL]]()
//      appPaths.add(appClasspaths.asJava)
//      val r = classOf[Bootstrap].getClassLoader.asInstanceOf[URLClassLoader]
//      val urlStrs = (r.getURLs.toList ++: appClasspaths).map(i => i.getPath)
//
//      Bootstrap.main(urlStrs.toArray)

      // val applicationClassLoader = new ApplicationClassLoader(appClasspaths.toList)
      Bootstrap.sbtStartup( this.getClass.getClassLoader,
        appClasspaths.toList.asJava
      )

    }
    catch {
      case ex: Exception => {
        println(ex.getStackTrace)
        logger.error(ex.getMessage, ex)
      }
    }


  }


}

