package org.scala.ide.m2e.scala

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.JavaCore
import org.eclipse.m2e.core.project.IMavenProjectFacade
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator
import org.eclipse.m2e.jdt.IClasspathDescriptor
import org.eclipse.m2e.jdt.IJavaProjectConfigurator
import org.scala.ide.m2e.internal.scala.ScalaClasspathUtils
import org.scala.ide.m2e.internal.scala.ScalaProjectUtils
import org.scala.ide.m2e.internal.slf4j.Logging
import org.scala.ide.m2e.internal.scala.ScalaWTPClasspathUtils

//TODO check the jre/java version compliance (>= 1.5)
//TODO check JDT Weaving is enabled (if not enabled, icon of scala file is [J] same as java (and property of  the file display "Type :... Java Source File" )
//TODO check that pom.xml and ScalaLib Container declare the same scala version
//TODO keep sync scala-compiler configuration between pom.xml and scala-plugin ? (sync bi-direction) ?
//TODO ask sonatype/mailing-list about how to retreive maven plugin configuration, like additional sourceDirectory

class ScalaProjectConfigurator extends AbstractJavaProjectConfigurator
with ScalaProjectUtils 
with ScalaClasspathUtils
with ScalaWTPClasspathUtils
with IJavaProjectConfigurator 
with Logging {


  override def configure(request: ProjectConfigurationRequest, monitor: IProgressMonitor): Unit = {
    if (request != null) {
      try {
        
        // check for proper scala IDE
        if(!hasScalaIde) {
          error( """Unable to detect a supported Scala IDE plugin. 
              Pleasure ensure you have the proper Scala IDE intalled.""")
          return 
        }
        
        val project = request.getProject()
        val facade = request.getMavenProjectFacade()
        
        if(!hasScalaNature(project) && hasScalaMojo(facade, monitor)) {
          if(!hasJavaNature(project)) {
            debug("Adding Java project nature.")
            addJavaNature(project, monitor)
          }
          debug("Adding Scala project nature.")
          addScalaNature(project, monitor)
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
  override def configureClasspath(facade: IMavenProjectFacade, classpath: IClasspathDescriptor, monitor: IProgressMonitor): Unit = {

    val hasNature = hasScalaNature(facade.getProject())
    val isLaunchContext = isLaunchConfigContext()
    val hasMojo = hasScalaMojo(facade, monitor)

    // Remove Scala jars from Maven classpath
    if (hasNature == true && isLaunchContext == false && hasMojo) {
      
      // TODO ensure source folder exists (scala ide will error out if not)
      // TODO ensure test folder exists (scala ide will error out if not)
      
      val javaProject = JavaCore.create(facade.getProject())
      val entries = javaProject.getRawClasspath()
      
      debug("Searching for and removing Scala Jars from Maven classpath.")
      removeScalaLibs(classpath)
      
      debug("Adding default Scala source paths")
      val nEntries = addSourceDirs(facade,entries)
      
      debug("Sorting Scala project classpath.")
      val nnEntries = sortClasspath(nEntries)
      javaProject.setRawClasspath(nnEntries,monitor)
      
      // modify scala classpath state for WTP
      makeScalaLibDeployable(facade, classpath, monitor)
    }
  }
  
}






