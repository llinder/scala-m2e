package org.scala.ide.m2e.internal.scala

import scala.collection.JavaConversions._
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator
import org.eclipse.m2e.core.project.IMavenProjectFacade
import org.eclipse.m2e.jdt.IClasspathDescriptor
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.IClasspathAttribute

trait ScalaProjectUtils extends AbstractProjectConfigurator {

  object ScalaConst {
    val SCALA_NATURE = "org.scala-ide.sdt.core.scalanature"
    val MOJO_GROUP_ID = "org.scala-tools"
    val MOJO_ARTIFACT_ID = "maven-scala-plugin"
  }

  /**
   * Helper for checking we are running within a launch configuration.
   */
  protected def isLaunchConfigContext(): Boolean = {
    val elements = Thread.currentThread().getStackTrace().toList
    return elements.exists(p => "launch".equals(p.getMethodName()))
  }

  protected def hasScalaMojo(facade: IMavenProjectFacade, monitor: IProgressMonitor): Boolean = {

    val plugins = facade.getMavenProject(monitor).getBuildPlugins();

    return plugins.exists {
      p =>
        (p.getGroupId(), p.getArtifactId(), p.getExecutions().isEmpty()) match {
          case (ScalaConst.MOJO_GROUP_ID, ScalaConst.MOJO_ARTIFACT_ID, false) => true
          case _ => false
        }
    }

  }

  protected def hasScalaNature(project: IProject): Boolean = {
    return project.hasNature(ScalaConst.SCALA_NATURE)
  }

  protected def addScalaNature(project: IProject, monitor: IProgressMonitor): Unit = {
    AbstractProjectConfigurator.addNature(project, ScalaConst.SCALA_NATURE, monitor)
  }
}

trait ScalaClasspathUtils extends AbstractProjectConfigurator {

  object ScalaClasspathUtils {
    val SOURCE_WEIGHT = Map[String, Int]("src/main/" -> 90000, "src/test/" -> 10000, "org.scala-ide.sdt.launching.SCALA_CONTAINER" -> 3000, "org.eclipse.jdt.launching.JRE_CONTAINER" -> 2000, "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER" -> 1000)
    val RESOURCE_WEIGHT = Map[String, Int]("java" -> 100, "resources" -> -10)
  }

  protected def addSourceDirs(facade: IMavenProjectFacade, entries: Array[IClasspathEntry]): Array[IClasspathEntry] = {
    val project = facade.getProject()
    val output = facade.getOutputLocation()

    val mainSrc = project.getFolder("src/main/scala")
    val testSrc = project.getFolder("src/test/scala")

    def add(source: IFolder, output: IPath, entries: Array[IClasspathEntry]): Array[IClasspathEntry] = {
      val sourcePath = source.getFullPath()
      if (source.exists()) {
        val entry = JavaCore.newSourceEntry(sourcePath, new Array[IPath](0), new Array[IPath](0), output, new Array[IClasspathAttribute](0));
        val exists = entries.exists {
          e => e.getPath() equals entry.getPath()
        }

        if (exists == false)
          entries :+ entry
        else
          entries

      } else {
        entries
      }

    }

    add(testSrc, output, add(mainSrc, output, entries))
  }

  protected def sortClasspath(classpath: Array[IClasspathEntry]): Array[IClasspathEntry] = {

    // Utility function to get the sort weight for the given entry
    def weight(entry: IClasspathEntry): Int = {

      if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE || entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {

        val entryPath = entry.getPath().toString();

        // Sum all source and resource weights
        val srcWeight = ScalaClasspathUtils.SOURCE_WEIGHT.foldLeft(0) {
          (r, c) => if (entryPath.contains(c._1)) r + c._2 else r
        }
        val totalWeight = ScalaClasspathUtils.RESOURCE_WEIGHT.foldLeft(srcWeight) {
          (r, c) => if (entryPath.contains(c._1)) r + c._2 else r
        }

        return totalWeight

      } else {
        return 0
      }
    }

    // Sort class path based on weights / paths
    val sourted = classpath.toList.sort {
      (a, b) =>
        {
          val aw = weight(a)
          val bw = weight(b)

          if (aw > 0 || bw > 0)
            if (aw equals bw)
              ((a.getPath.toString compareTo b.getPath.toString) < 0)
            else
              ((aw compareTo bw) >= 0)
          else
            false
        }
    }

    return sourted.toArray
  }

  protected def removeScalaLibs(classpath: IClasspathDescriptor): Unit = {
    classpath.removeEntry(new ScalaEntityFilter())
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

}