package org.scala.ide.m2e.scala

import scala.collection.JavaConversions._
import org.apache.maven.model.Plugin
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.IClasspathAttribute
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.JavaCore
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest
import org.eclipse.m2e.core.project.IMavenProjectFacade
import org.eclipse.m2e.jdt.IClasspathDescriptor
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor
import org.scala.ide.m2e.internal.slf4j.Logging
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.m2e.jdt.IJavaProjectConfigurator
import org.scala.ide.m2e.internal.scala.ScalaProjectUtils
import org.scala.ide.m2e.internal.scala.ScalaClasspathUtils

//TODO check the jre/java version compliance (>= 1.5)
//TODO check JDT Weaving is enabled (if not enabled, icon of scala file is [J] same as java (and property of  the file display "Type :... Java Source File" )
//TODO check that pom.xml and ScalaLib Container declare the same scala version
//TODO keep sync scala-compiler configuration between pom.xml and scala-plugin ? (sync bi-direction) ?
//TODO ask sonatype/mailing-list about how to retreive maven plugin configuration, like additional sourceDirectory

class ScalaProjectConfigurator extends AbstractProjectConfigurator with ScalaProjectUtils with ScalaClasspathUtils with IJavaProjectConfigurator with Logging {


  def configure(request: ProjectConfigurationRequest, monitor: IProgressMonitor): Unit = {
    if (request != null) {
      try {
        val project = request.getProject()
        val facade = request.getMavenProjectFacade()

        val hasNature = hasScalaNature(project)
        val hasMojo = hasScalaMojo(facade, monitor)

        // Handle scala nature
        (hasNature, hasMojo) match {
          case (false, true) => {
            debug("Adding Scala project nature.")
            addScalaNature(project, monitor)
          }
          case (true, false) => {
            // TODO should scala nature be removed??
          }
          case (false, false) => {
            error("""Invalid Maven Scala configuration. \n
            Expected Scala Mojo with executions defined but found none. \n
            Please ensure Scala mojo is defined on the POM and has atleast one execution.""")
          }
          case _ => Nil
        }
      } catch {
        case e: Exception => error("Error configuration Scala project.", e)
      }
    }
  }

  /**
   * Configures *Maven* project classpath, i.e. content of Maven Dependencies classpath container.
   */
  @throws(classOf[CoreException])
  def configureClasspath(facade: IMavenProjectFacade, classpath: IClasspathDescriptor, monitor: IProgressMonitor): Unit = {

    val hasNature = hasScalaNature(facade.getProject())
    val isLaunchContext = isLaunchConfigContext()
    val hasMojo = hasScalaMojo(facade, monitor)

    // Remove Scala jars from Maven classpath
    if (hasNature == true && isLaunchContext == false && hasMojo) {
      
      val javaProject = JavaCore.create(facade.getProject())
      val entries = javaProject.getRawClasspath()
      
      debug("Searching for and removing Scala Jars from Maven classpath.")
      removeScalaLibs(classpath)
      
      debug("Adding default Scala source paths")
      val nEntries = addSourceDirs(facade,entries)
      
      debug("Sorting Scala project classpath.")
      val nnEntries = sortClasspath(nEntries)
      
      javaProject.setRawClasspath(nnEntries,monitor)
    }
  }

  /**
   * Configures *JDT* project classpath, i.e. project-level entries like source folders, JRE and Maven Dependencies
   * classpath container.
   */
  @throws(classOf[CoreException])
  def configureRawClasspath(request: ProjectConfigurationRequest, classpath: IClasspathDescriptor, monitor: IProgressMonitor): Unit = {
    // noop
  }
  
}






