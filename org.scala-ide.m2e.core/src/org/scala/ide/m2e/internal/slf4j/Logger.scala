package org.scala.ide.m2e.internal.slf4j

import org.slf4j.{Logger => SLF4JLogger}

class Logger(val logger: SLF4JLogger) {
  import scala.language.implicitConversions._

  @inline final def name = logger.getName

  @inline final def isTraceEnabled = logger.isTraceEnabled

  @inline final def trace(msg: => Any): Unit =
    if (isTraceEnabled) logger.trace(msg.toString)

  @inline final def trace(msg: => Any, t: => Throwable): Unit =
    if (isTraceEnabled) logger.trace(msg, t)

  @inline final def isDebugEnabled = logger.isDebugEnabled

  @inline final def debug(msg: => Any): Unit =
    if (isDebugEnabled) logger.debug(msg.toString)

  @inline final def debug(msg: => Any, t: => Throwable): Unit =
    if (isDebugEnabled) logger.debug(msg, t)

  @inline final def isErrorEnabled = logger.isErrorEnabled

  @inline final def error(msg: => Any): Unit =
    if (isErrorEnabled) logger.error(msg.toString)

  @inline final def error(msg: => Any, t: => Throwable): Unit =
    if (isErrorEnabled) logger.error(msg, t)

  @inline final def isInfoEnabled = logger.isInfoEnabled

  @inline final def info(msg: => Any): Unit =
    if (isInfoEnabled) logger.info(msg.toString)

  @inline final def info(msg: => Any, t: => Throwable): Unit =
    if (isInfoEnabled) logger.info(msg, t)

  @inline final def isWarnEnabled = logger.isWarnEnabled

  @inline final def warn(msg: => Any): Unit =
    if (isWarnEnabled) logger.warn(msg.toString)

  @inline final def warn(msg: => Any, t: => Throwable): Unit =
    if (isWarnEnabled) logger.warn(msg, t)

  private implicit def _any2String(msg: Any): String =
    msg match {
      case null => "<null>"
      case _    => msg.toString
    }
}

trait Logging {

  private lazy val _logger = Logger(getClass)

  protected def logger: Logger = _logger

  protected def loggerName = logger.name

  protected def isTraceEnabled = logger.isTraceEnabled

  protected def trace(msg: => Any): Unit = logger.trace(msg)

  protected def trace(msg: => Any, t: => Throwable): Unit =
    logger.trace(msg, t)

  protected def isDebugEnabled = logger.isDebugEnabled

  protected def debug(msg: => Any): Unit = logger.debug(msg)

  protected def debug(msg: => Any, t: => Throwable): Unit =
    logger.debug(msg, t)

  protected def isErrorEnabled = logger.isErrorEnabled

  protected def error(msg: => Any): Unit = logger.error(msg)

  protected def error(msg: => Any, t: => Throwable): Unit =
    logger.error(msg, t)

  protected def isInfoEnabled = logger.isInfoEnabled

  protected def info(msg: => Any): Unit = logger.info(msg)

  protected def info(msg: => Any, t: => Throwable): Unit =
    logger.info(msg, t)

  protected def isWarnEnabled = logger.isWarnEnabled

  protected def warn(msg: => Any): Unit = logger.warn(msg)

  protected def warn(msg: => Any, t: => Throwable): Unit =
    logger.warn(msg, t)
}

object Logger {
  import scala.reflect.{classTag, ClassTag}

  val RootLoggerName = SLF4JLogger.ROOT_LOGGER_NAME

  def apply(name: String): Logger =
    new Logger(org.slf4j.LoggerFactory.getLogger(name))

  def apply(cls: Class[_]): Logger = apply(cls.getName)

  def apply[C: ClassTag](): Logger = apply(classTag[C].runtimeClass.getName)

  def rootLogger = apply(RootLoggerName)
}