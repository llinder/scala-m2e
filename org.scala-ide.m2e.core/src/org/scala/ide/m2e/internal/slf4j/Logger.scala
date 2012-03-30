package org.scala.ide.m2e.internal.slf4j

import org.slf4j.{ Logger => SLF4JLogger }

class Logger(val logger: SLF4JLogger) {

  @inline final def name = logger.getName;

  @inline final def isTraceEnabled = logger.isTraceEnabled;

  @inline final def trace(message: => Any): Unit =
    if (isTraceEnabled) logger.trace(message);

  @inline final def trace(message: => Any, t: => Throwable): Unit =
    if (isTraceEnabled) logger.trace(message, t);

  @inline final def isDebugEnabled = logger.isDebugEnabled

  @inline final def debug(message: => Any): Unit =
    if (isDebugEnabled) logger.debug(message);

  @inline final def debug(message: => Any, t: => Throwable): Unit =
    if (isDebugEnabled) logger.debug(message, t);

  @inline final def isErrorEnabled = logger.isErrorEnabled

  @inline final def error(message: => Any): Unit =
    if (isErrorEnabled) logger.error(message);

  @inline final def error(message: => Any, t: => Throwable): Unit =
    if (isErrorEnabled) logger.error(message, t);

  @inline final def isInfoEnabled = logger.isInfoEnabled

  @inline final def info(message: => Any): Unit =
    if (isInfoEnabled) logger.info(message);

  @inline final def info(message: => Any, t: => Throwable): Unit =
    if (isInfoEnabled) logger.info(message, t);

  @inline final def isWarnEnabled = logger.isWarnEnabled;

  @inline final def warn(message: => Any): Unit =
    if (isWarnEnabled) logger.warn(message);

  @inline final def warn(message: => Any, t: => Throwable): Unit =
    if (isWarnEnabled) logger.warn(message, t);

  private implicit def convert2String(message: Any): String =
    message match {
      case null => "<null>"
      case _ => message.toString
    }
}

trait Logging {

  private lazy val _logger = Logger(getClass);

  protected def logger: Logger = _logger;

  protected def loggerName = logger.name;

  protected def isTraceEnabled = 
    logger.isTraceEnabled;

  protected def trace(message: => Any): Unit = 
    logger.trace(message);

  protected def trace(message: => Any, t: => Throwable): Unit =
    logger.trace(message, t);

  protected def isDebugEnabled = 
    logger.isDebugEnabled;

  protected def debug(message: => Any): Unit =
    logger.debug(message);

  protected def debug(message: => Any, t: => Throwable): Unit =
    logger.debug(message, t);

  protected def isErrorEnabled = 
    logger.isErrorEnabled;

  protected def error(message: => Any): Unit = 
    logger.error(message);

  protected def error(message: => Any, t: => Throwable): Unit =
    logger.error(message, t);

  protected def isInfoEnabled = 
    logger.isInfoEnabled;

  protected def info(message: => Any): Unit = 
    logger.info(message);

  protected def info(message: => Any, t: => Throwable): Unit =
    logger.info(message, t);

  protected def isWarnEnabled = 
    logger.isWarnEnabled;

  protected def warn(message: => Any): Unit = 
    logger.warn(message);

  protected def warn(message: => Any, t: => Throwable): Unit =
    logger.warn(message, t);
}

object Logger {

  val RootLoggerName = 
    SLF4JLogger.ROOT_LOGGER_NAME;

  def apply(name: String): Logger =
    new Logger(org.slf4j.LoggerFactory.getLogger(name));

  def apply(cls: Class[_]): Logger = 
    apply(cls.getName);

  def apply[C](implicit m: Manifest[C]): Logger = 
    apply(m.erasure.getName);

  def rootLogger = 
    apply(RootLoggerName);
}