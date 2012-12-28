package org.scala.ide.m2e.internal.scala

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.IClasspathAttribute
import org.eclipse.m2e.jdt.IClasspathDescriptor
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.m2e.core.project.IMavenProjectFacade
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator
import org.eclipse.jdt.internal.core.ClasspathAttribute

trait ScalaWTPClasspathUtils extends AbstractJavaProjectConfigurator {

  protected def makeScalaLibDeployable(facade: IMavenProjectFacade, classpath: IClasspathDescriptor, monitor: IProgressMonitor): Unit = {
    val project = JavaCore.create(facade.getProject())
    if (project == null)
      return

    val scalaLibrary = getContainer(project, PluginConst.SCALA_CONTAINER_PATH)

    if (scalaLibrary == None)
      return

    val mavenLibrary = getContainer(project, PluginConst.MAVEN_CONTAINER_PATH)

    val deployableAttr = getDeployableAttribute(mavenLibrary)
    // If the Maven Classpath Container is set to be deployed in WTP, then do the same for the Scala one
    if (deployableAttr.isDefined)
      // Add the deployable attribute only if it's not set already
      if (!getDeployableAttribute(scalaLibrary).isDefined)
        addDeployableAttribute(project, deployableAttr.get, monitor)

  }

  private def getContainer(project: IJavaProject, containerPath: String): Option[IClasspathEntry] = {
    val cpe = project.getRawClasspath()
    val ctrs = for (
      e <- cpe;
      if e.getEntryKind == IClasspathEntry.CPE_CONTAINER &&
        containerPath == e.getPath.lastSegment
    ) yield e
    if (ctrs.size > 0) Some(ctrs.head) else None
  }

  private def getDeployableAttribute(library: Option[IClasspathEntry]): Option[IClasspathAttribute] = {
    if (!library.isDefined) None

    val attrs = library.get.getExtraAttributes
    if (attrs == null || attrs.size == 0) None

    val ret = for (a <- attrs; if PluginConst.DEPLOYABLE_KEY == a.getName)
      yield new ClasspathAttribute(a.getName, a.getValue)

    if (ret.size > 0) Some(ret.head) else None
  }

  private def addDeployableAttribute(project: IJavaProject, attribute: IClasspathAttribute, monitor: IProgressMonitor): Unit = {
    if (project == null)
      return

    val scalaCp = getContainer(project, PluginConst.SCALA_CONTAINER_PATH)
    if (scalaCp.isDefined) {
      // collect all existing attributes except non deployable keys
      val attrs = for (
        ae <- scalaCp.get.getExtraAttributes;
        if PluginConst.NON_DEPLOYABLE_KEY != ae.getName &&
          ae.getName != attribute.getName
      ) yield ae

      val nAttrs = attrs :+ attribute
      val newCp = JavaCore.newContainerEntry(scalaCp.get.getPath, scalaCp.get.getAccessRules, nAttrs, scalaCp.get.isExported)
      // splice new classpath in place of the old one
      val cp = project.getRawClasspath().map(e =>
        if (e.getEntryKind == IClasspathEntry.CPE_CONTAINER &&
          e.getPath.lastSegment == PluginConst.SCALA_CONTAINER_PATH)
          scalaCp.get else e)

      // set new classpath on java project
      project.setRawClasspath(cp, monitor)
    }

  }

}