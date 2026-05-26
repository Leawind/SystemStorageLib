package io.github.leawind.systemstoragelib.v1.impl.log;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

public class SystemLogger implements Logger {
  private static final Logger SLF4J_LOGGER = LoggerFactory.getLogger(SystemLogger.class);

  private final LogManager logManager;
  private final String scopeName;
  private final long pid;

  public SystemLogger(LogManager logManager, String scopeName) {
    this.logManager = logManager;
    this.scopeName = scopeName;
    this.pid = ProcessHandle.current().pid();
  }

  @Override
  public String getName() {
    return scopeName;
  }

  // region Log Methods

  // region Trace

  @Override
  public boolean isTraceEnabled() {
    return true;
  }

  @Override
  public boolean isTraceEnabled(@Nullable Marker marker) {
    return true;
  }

  @Override
  public void trace(String msg) {
    logManager.writeLog(Level.TRACE, scopeName, pid, msg);
    SLF4J_LOGGER.trace(msg);
  }

  @Override
  public void trace(String format, Object arg) {
    logManager.writeLog(
        Level.TRACE, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.trace(format, arg);
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.TRACE, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.trace(format, arg1, arg2);
  }

  @Override
  public void trace(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.TRACE, scopeName, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
    SLF4J_LOGGER.trace(format, arguments);
  }

  @Override
  public void trace(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.TRACE, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.trace(msg, t);
  }

  @Override
  public void trace(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.TRACE, scopeName, pid, msg);
    SLF4J_LOGGER.trace(marker, msg);
  }

  @Override
  public void trace(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(
        Level.TRACE, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.trace(marker, format, arg);
  }

  @Override
  public void trace(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.TRACE, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.trace(marker, format, arg1, arg2);
  }

  @Override
  public void trace(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.TRACE, scopeName, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
    SLF4J_LOGGER.trace(marker, format, argArray);
  }

  @Override
  public void trace(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.TRACE, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.trace(marker, msg, t);
  }

  // endregion

  // region Debug

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public boolean isDebugEnabled(@Nullable Marker marker) {
    return true;
  }

  @Override
  public void debug(String msg) {
    logManager.writeLog(Level.DEBUG, scopeName, pid, msg);
    SLF4J_LOGGER.debug(msg);
  }

  @Override
  public void debug(String format, Object arg) {
    logManager.writeLog(
        Level.DEBUG, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.debug(format, arg);
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.DEBUG, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.debug(format, arg1, arg2);
  }

  @Override
  public void debug(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.DEBUG, scopeName, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
    SLF4J_LOGGER.debug(format, arguments);
  }

  @Override
  public void debug(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.DEBUG, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.debug(msg, t);
  }

  @Override
  public void debug(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.DEBUG, scopeName, pid, msg);
    SLF4J_LOGGER.debug(marker, msg);
  }

  @Override
  public void debug(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(
        Level.DEBUG, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.debug(marker, format, arg);
  }

  @Override
  public void debug(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.DEBUG, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.debug(marker, format, arg1, arg2);
  }

  @Override
  public void debug(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.DEBUG, scopeName, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
    SLF4J_LOGGER.debug(marker, format, argArray);
  }

  @Override
  public void debug(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.DEBUG, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.debug(marker, msg, t);
  }

  // endregion

  // region Info

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public boolean isInfoEnabled(@Nullable Marker marker) {
    return true;
  }

  @Override
  public void info(String msg) {
    logManager.writeLog(Level.INFO, scopeName, pid, msg);
    SLF4J_LOGGER.info(msg);
  }

  @Override
  public void info(String format, Object arg) {
    logManager.writeLog(
        Level.INFO, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.info(format, arg);
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.INFO, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.info(format, arg1, arg2);
  }

  @Override
  public void info(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.INFO, scopeName, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
    SLF4J_LOGGER.info(format, arguments);
  }

  @Override
  public void info(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.INFO, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.info(msg, t);
  }

  @Override
  public void info(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.INFO, scopeName, pid, msg);
    SLF4J_LOGGER.info(marker, msg);
  }

  @Override
  public void info(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(
        Level.INFO, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.info(marker, format, arg);
  }

  @Override
  public void info(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.INFO, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.info(marker, format, arg1, arg2);
  }

  @Override
  public void info(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.INFO, scopeName, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
    SLF4J_LOGGER.info(marker, format, argArray);
  }

  @Override
  public void info(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.INFO, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.info(marker, msg, t);
  }

  // endregion

  // region Warn

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public boolean isWarnEnabled(@Nullable Marker marker) {
    return true;
  }

  @Override
  public void warn(String msg) {
    logManager.writeLog(Level.WARN, scopeName, pid, msg);
    SLF4J_LOGGER.warn(msg);
  }

  @Override
  public void warn(String format, Object arg) {
    logManager.writeLog(
        Level.WARN, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.warn(format, arg);
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.WARN, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.warn(format, arg1, arg2);
  }

  @Override
  public void warn(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.WARN, scopeName, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
    SLF4J_LOGGER.warn(format, arguments);
  }

  @Override
  public void warn(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.WARN, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.warn(msg, t);
  }

  @Override
  public void warn(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.WARN, scopeName, pid, msg);
    SLF4J_LOGGER.warn(marker, msg);
  }

  @Override
  public void warn(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(
        Level.WARN, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.warn(marker, format, arg);
  }

  @Override
  public void warn(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.WARN, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.warn(marker, format, arg1, arg2);
  }

  @Override
  public void warn(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.WARN, scopeName, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
    SLF4J_LOGGER.warn(marker, format, argArray);
  }

  @Override
  public void warn(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.WARN, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.warn(marker, msg, t);
  }

  // endregion

  // region Error

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public boolean isErrorEnabled(@Nullable Marker marker) {
    return true;
  }

  @Override
  public void error(String msg) {
    logManager.writeLog(Level.ERROR, scopeName, pid, msg);
    SLF4J_LOGGER.error(msg);
  }

  @Override
  public void error(String format, Object arg) {
    logManager.writeLog(
        Level.ERROR, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.error(format, arg);
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.ERROR, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.error(format, arg1, arg2);
  }

  @Override
  public void error(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.ERROR, scopeName, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
    SLF4J_LOGGER.error(format, arguments);
  }

  @Override
  public void error(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.ERROR, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.error(msg, t);
  }

  @Override
  public void error(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.ERROR, scopeName, pid, msg);
    SLF4J_LOGGER.error(marker, msg);
  }

  @Override
  public void error(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(
        Level.ERROR, scopeName, pid, MessageFormatter.format(format, arg).getMessage());
    SLF4J_LOGGER.error(marker, format, arg);
  }

  @Override
  public void error(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.ERROR, scopeName, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
    SLF4J_LOGGER.error(marker, format, arg1, arg2);
  }

  @Override
  public void error(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.ERROR, scopeName, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
    SLF4J_LOGGER.error(marker, format, argArray);
  }

  @Override
  public void error(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.ERROR, scopeName, pid, appendThrowable(msg, t));
    SLF4J_LOGGER.error(marker, msg, t);
  }

  // endregion

  // endregion Log Methods

  private static String appendThrowable(String msg, @Nullable Throwable t) {
    if (t == null) return msg;
    return msg + " " + t;
  }
}
