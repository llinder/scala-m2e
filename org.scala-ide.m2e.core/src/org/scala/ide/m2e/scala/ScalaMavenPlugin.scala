package org.scala.ide.m2e.scala

import org.eclipse.core.runtime.Plugin
import org.osgi.framework.BundleContext
import org.eclipse.core.runtime.Platform
import org.osgi.framework.Bundle
import org.slf4j.LoggerFactory
import org.scala.ide.m2e.internal.slf4j.Logging

object ScalaMavenPlugin {
  val SCALA_BUNDLE = "org.scala-ide.sdt.core";

  private var plugin: ScalaMavenPlugin = null;

  def getDefault() = plugin
  
  private var _hasScalaIde:Option[Boolean] = None
}

class ScalaMavenPlugin extends Plugin with Logging {
  
  import ScalaMavenPlugin._

  val log = LoggerFactory.getLogger(classOf[ScalaMavenPlugin])

  @throws(classOf[Exception])
  override def start(context: BundleContext): Unit = {
    debug("String Maven Integration for Scala IDE")

    ScalaMavenPlugin.plugin = this;

    _hasScalaIde = Some(hasScalaIde(context))
    
    if (!hasScalaIde)
      error("""Unable to detect a supported Scala IDE plugin. 
          Pleasure ensure you have the proper Scala IDE intalled.""")

  }
  
  def hasScalaIde = _hasScalaIde.getOrElse( false )

  private def hasScalaIde(context:BundleContext) = {
    _hasScalaIde.getOrElse {
      val bs = context.getBundles()
      val names = for( b <- bs; if b.getSymbolicName() == SCALA_BUNDLE) 
        yield b.getSymbolicName()
      names.size > 0
    }
  }

}