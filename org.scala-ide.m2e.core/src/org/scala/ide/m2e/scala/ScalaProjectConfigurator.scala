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


//TODO check the jre/java version compliance (>= 1.5)
//TODO check JDT Weaving is enabled (if not enabled, icon of scala file is [J] same as java (and property of  the file display "Type :... Java Source File" )
//TODO check that pom.xml and ScalaLib Container declare the same scala version
//TODO keep sync scala-compiler configuration between pom.xml and scala-plugin ? (sync bi-direction) ?
//TODO ask sonatype/mailing-list about how to retreive maven plugin configuration, like additional sourceDirectory

class ScalaProjectConfigurator extends AbstractProjectConfigurator with Logging {

  object Conf {
    val SCALA_NATURE = "org.scala-ide.sdt.core.scalanature"

    val MOJO_GROUP_ID = "org.scala-tools"

    val MOJO_ARTIFACT_ID = "maven-scala-plugin";

    val SOURCE_WEIGHT = Map[String, Int]("src/main/" -> 9000, "src/test/" -> 1000)

    val RESOURCE_WEIGHT = Map[String, Int]("java" -> 100, "resources" -> -10)
  }

  def configure(request: ProjectConfigurationRequest, monitor: IProgressMonitor): Unit = {

    if (request != null) {
      // Handle Scala nature configuration
      debug("Adding Scala nature " + Conf.SCALA_NATURE)

      try {
        val project = request.getProject()

        // Add Scala nature if it doesn't already exist and a valid Maven plugin is defined.
        (project.hasNature(Conf.SCALA_NATURE), isScalaProject(request.getMavenProjectFacade(), monitor)) match {
          case (false, true) => AbstractProjectConfigurator.addNature(project, Conf.SCALA_NATURE, monitor)
          case (false, false) => error("A valid Maven Scala plugin was not found on the POM.")
          case _ => Nil
        }

      } catch {
        case e: Exception => error("Error adding Scala project nature.", e)
      }

    }

  }

  def configureClasspath(facade: IMavenProjectFacade, classpath: IClasspathDescriptor, monitor: IProgressMonitor): Unit = {

    val project = facade.getProject()
    val hasScalaNature = project.hasNature(Conf.SCALA_NATURE)
    if (hasScalaNature == false) {
      // TODO add problem marker
      error("Class path configuration failed because Scala IDE doesn't exist.")
    } else if (isLaunchConfigurationCtx == false) {
      if (isScalaProject(facade, monitor)) {
        removeScalaFromMavenContainer(classpath)
        addDefaultScalaSourceDirs(facade, classpath, monitor)
        sortContainerScalaJre(project, monitor)
      }
    }

    // TODO there must be a better way to check if this is a launch
    def isLaunchConfigurationCtx(): Boolean = {
      val l = Thread.currentThread().getStackTrace().toList
      return l.exists(p => "launch".equals(p.getMethodName()))
    }

  }

  /**
   * TODO this seems more complicated than necessary... can we do better??
   * 
   * Helper method to reorder Containers
   *
   * "Scala Lib" should be before "JRE Sys", else 'Run Scala Application' set Boot
   * Entries JRE before Scala and failed with scala.* NotFound Exception. Should already be done when adding nature.
   *
   * @see scala.tools.eclipse.Nature#configure()
   */
  @throws(classOf[CoreException])
  private def sortContainerScalaJre(project: IProject, monitor: IProgressMonitor): Unit = {
    val javaProject = JavaCore.create(project)
    val classpath = javaProject.getRawClasspath()

    def weight(entry: IClasspathEntry): Int = {
      return if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {

        val entryPath = entry.getPath().toString();

        val sw = Conf.SOURCE_WEIGHT.count {
          x => entryPath.contains(x._1)
        }

        val rw = Conf.RESOURCE_WEIGHT.count {
          x => entryPath.endsWith(x._1)
        }

        return sw + rw

      } else {
        return 0;
      }
    }

    val sorted = classpath.toList.sort((A, B) => {

      val aWeight = weight(A)
      val bWeight = weight(B)

      def comp(): Boolean = {
        return if (aWeight > 0 || bWeight > 0) {
          if (aWeight equals bWeight) {
            (A.getPath().toString() compareTo B.getPath().toString()) < 0
          } else {
            (aWeight compareTo bWeight) < 0
          }
        } else {
          false
        }
      }

      comp()

    })
    
    javaProject.setRawClasspath( classpath.toArray, monitor )

  }

  /**
   * Helper method that removed Scala artifacts from Maven
   * container when they already exist in the Scala container
   */
  private def removeScalaFromMavenContainer(mvnClasspath: IClasspathDescriptor): Unit = {
    mvnClasspath.removeEntry(new ScalaEntityFilter())
  }

  /**
   * Helper method that checks for existence of Scala
   * source paths and adds them to the class path if they exist.
   */
  private def addDefaultScalaSourceDirs(facade: IMavenProjectFacade, classpath: IClasspathDescriptor, monitor: IProgressMonitor): Unit = {

    // get raw class path
    val project = facade.getProject()
    val javaProject = JavaCore.create(project)
    val rawClasspath = javaProject.getRawClasspath()

    // helper function for adding given folder to class path in the proper order
    def add(classpath: Array[IClasspathEntry], folder: IFolder, output: IPath): Array[IClasspathEntry] = {

      def doAdd(classpath: List[IClasspathEntry], source: IPath, output: IPath): List[IClasspathEntry] = {

        val entry = JavaCore.newSourceEntry(source, new Array[IPath](0), new Array[IPath](0), output, new Array[IClasspathAttribute](0))

        val has = classpath.exists(e => e.equals(entry))

        return if (has == false)
          entry :: classpath
        else
          classpath
      }

      return if (folder != null && folder.exists()) {
        val p = folder.getFullPath()
        doAdd(classpath.toList, p, output).toArray
      } else {
        classpath
      }
    }

    // don't use classpath.addSourceEntry because source entry are append under "Maven Dependencies" container
    val defaultMainSrc = project.getFolder("src/main/scala")
    val cp1 = add(rawClasspath, defaultMainSrc, facade.getOutputLocation())

    val defaultTestSrc = project.getFolder("src/test/scala")
    val cp2 = add(cp1, defaultMainSrc, facade.getOutputLocation())

    // set new classpath if entries have been added
    if (cp2.size != rawClasspath.size)
      javaProject.setRawClasspath(cp2, monitor)
  }

  /**
   * Checks the given Maven project for a valid Scala Maven plugin.
   * If found returns true otherwise false.
   *
   * @return
   */
  @throws(classOf[CoreException])
  private def isScalaProject(facade: IMavenProjectFacade, monitor: IProgressMonitor): Boolean = {

    val plugins = facade.getMavenProject(monitor).getBuildPlugins();

    // Function to check if the Maven project has a Scala Plugin defined.
    def hasPlugin(plugin: Plugin): Boolean = {
      (plugin.getGroupId(), plugin.getArtifactId(), plugin.getExecutions().isEmpty()) match {
        case (Conf.MOJO_GROUP_ID, Conf.MOJO_ARTIFACT_ID, false) => true
        case _ => false
      }
    }

    for (plugin <- plugins)
      if (hasPlugin(plugin))
        return true

    return false
  }

}

class ScalaEntityFilter extends IClasspathDescriptor.EntryFilter {

  // TODO use values from Scala Library Container instead of hard coded value
  override def accept(entry: IClasspathEntryDescriptor): Boolean = {
    (entry.getGroupId(), entry.getArtifactId()) match {
      case ("org.scala-lang", "scala-library") => true
      case ("org.scala-lang", "scala-dbc") => true
      case ("org.scala-lang", "scala-swing") => true
      case _ => false
    }
  }
}




