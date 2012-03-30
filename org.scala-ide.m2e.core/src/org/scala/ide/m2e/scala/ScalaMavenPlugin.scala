package org.scala.ide.m2e.scala

import org.eclipse.core.runtime.Plugin
import org.osgi.framework.BundleContext
import org.eclipse.core.runtime.Platform
import org.osgi.framework.Bundle
import org.slf4j.LoggerFactory
import org.scala.ide.m2e.internal.slf4j.Logging

class ScalaMavenPlugin extends Plugin with Logging {

  val log = LoggerFactory.getLogger(classOf[ScalaMavenPlugin])

  object ScalaMavenPlugin {
    val SCALA_BUNDLE = "org.scala-ide.sdt.core";

    var plugin: ScalaMavenPlugin = null;

    var scalaIdeBundle: Bundle = null;
  }

  @throws(classOf[Exception])
  override def start(context: BundleContext): Unit = {
    
    debug( "String Maven Integration for Scala IDE" )
    
    ScalaMavenPlugin.plugin = this;
    ScalaMavenPlugin.scalaIdeBundle = Platform.getBundle(ScalaMavenPlugin.SCALA_BUNDLE)
    
    if( ScalaMavenPlugin.scalaIdeBundle == null )
      error( "Unable to detect a supported Scala IDE plugin.")
      
  }
}