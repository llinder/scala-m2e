package org.scala.ide.m2e.internal.scala

import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator
import org.eclipse.core.resources.IProject
import org.eclipse.m2e.core.project.IMavenProjectFacade
import org.scala.ide.m2e.scala.ScalaMavenPlugin
import java.io.File
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator
import scala.collection.JavaConversions._

trait ScalaProjectUtils extends AbstractJavaProjectConfigurator {

  /**
   * Helper for checking we are running within a launch configuration.
   */
  protected def isLaunchConfigContext(): Boolean = {
    val elements = Thread.currentThread().getStackTrace().toList
    return elements.exists(p => "launch".equals(p.getMethodName()))
  }
  
  // workaround to avoid NPE
  override protected def getFullPath( facade:IMavenProjectFacade, file:File ):IPath = {
    if (file == null) 
      null 
    else
      super.getFullPath(facade, file)
  }

  protected def hasScalaMojo(facade: IMavenProjectFacade, monitor: IProgressMonitor): Boolean = {

    val plugins = facade.getMavenProject(monitor).getBuildPlugins();

    return plugins.exists {
      p =>
        (p.getGroupId(), p.getArtifactId(), p.getExecutions().isEmpty()) match {
          case (PluginConst.MOJO_S_GROUP_ID, PluginConst.MOJO_S_ARTIFACT_ID, false) => true
          case (PluginConst.MOJO_A_GROUP_ID, PluginConst.MOJO_A_ARTIFACT_ID, false) => true
          case _ => false
        }
    }

  }

  protected def hasScalaIde(): Boolean = {
    ScalaMavenPlugin.getDefault.hasScalaIde
  }

  protected def hasJavaNature(project: IProject): Boolean = {
    return project.hasNature(PluginConst.JAVA_NATURE)
  }

  protected def hasScalaNature(project: IProject): Boolean = {
    return project.hasNature(PluginConst.SCALA_NATURE)
  }

  protected def addJavaNature(project: IProject, monitor: IProgressMonitor): Unit = {
    AbstractProjectConfigurator.addNature(project, PluginConst.JAVA_NATURE, monitor)
  }

  protected def addScalaNature(project: IProject, monitor: IProgressMonitor): Unit = {
    AbstractProjectConfigurator.addNature(project, PluginConst.SCALA_NATURE, monitor)
  }
  
}