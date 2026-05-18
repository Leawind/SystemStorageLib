package io.github.leawind.systemstoragelib.v1.impl.log;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

public class SystemLogger implements Logger {

  private final LogManager logManager;
  private final String scope;
  private final long pid;

  public SystemLogger(LogManager logManager, String scope) {
    this.logManager = logManager;
    this.scope = scope;
    this.pid = ProcessHandle.current().pid();
  }

  @Override
  public String getName() {
    return scope;
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
    logManager.writeLog(Level.TRACE, scope, pid, msg);
  }

  @Override
  public void trace(String format, Object arg) {
    logManager.writeLog(Level.TRACE, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.TRACE, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void trace(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.TRACE, scope, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  @Override
  public void trace(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.TRACE, scope, pid, appendThrowable(msg, t));
  }

  @Override
  public void trace(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.TRACE, scope, pid, msg);
  }

  @Override
  public void trace(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(Level.TRACE, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void trace(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.TRACE, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void trace(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.TRACE, scope, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
  }

  @Override
  public void trace(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.TRACE, scope, pid, appendThrowable(msg, t));
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
    logManager.writeLog(Level.DEBUG, scope, pid, msg);
  }

  @Override
  public void debug(String format, Object arg) {
    logManager.writeLog(Level.DEBUG, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.DEBUG, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void debug(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.DEBUG, scope, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  @Override
  public void debug(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.DEBUG, scope, pid, appendThrowable(msg, t));
  }

  @Override
  public void debug(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.DEBUG, scope, pid, msg);
  }

  @Override
  public void debug(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(Level.DEBUG, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void debug(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.DEBUG, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void debug(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.DEBUG, scope, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
  }

  @Override
  public void debug(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.DEBUG, scope, pid, appendThrowable(msg, t));
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
    logManager.writeLog(Level.INFO, scope, pid, msg);
  }

  @Override
  public void info(String format, Object arg) {
    logManager.writeLog(Level.INFO, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.INFO, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void info(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.INFO, scope, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  @Override
  public void info(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.INFO, scope, pid, appendThrowable(msg, t));
  }

  @Override
  public void info(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.INFO, scope, pid, msg);
  }

  @Override
  public void info(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(Level.INFO, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void info(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.INFO, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void info(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.INFO, scope, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
  }

  @Override
  public void info(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.INFO, scope, pid, appendThrowable(msg, t));
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
    logManager.writeLog(Level.WARN, scope, pid, msg);
  }

  @Override
  public void warn(String format, Object arg) {
    logManager.writeLog(Level.WARN, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.WARN, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void warn(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.WARN, scope, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  @Override
  public void warn(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.WARN, scope, pid, appendThrowable(msg, t));
  }

  @Override
  public void warn(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.WARN, scope, pid, msg);
  }

  @Override
  public void warn(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(Level.WARN, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void warn(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.WARN, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void warn(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.WARN, scope, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
  }

  @Override
  public void warn(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.WARN, scope, pid, appendThrowable(msg, t));
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
    logManager.writeLog(Level.ERROR, scope, pid, msg);
  }

  @Override
  public void error(String format, Object arg) {
    logManager.writeLog(Level.ERROR, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.ERROR, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void error(String format, Object @Nullable ... arguments) {
    logManager.writeLog(
        Level.ERROR, scope, pid, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  @Override
  public void error(String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.ERROR, scope, pid, appendThrowable(msg, t));
  }

  @Override
  public void error(@Nullable Marker marker, String msg) {
    logManager.writeLog(Level.ERROR, scope, pid, msg);
  }

  @Override
  public void error(@Nullable Marker marker, String format, Object arg) {
    logManager.writeLog(Level.ERROR, scope, pid, MessageFormatter.format(format, arg).getMessage());
  }

  @Override
  public void error(@Nullable Marker marker, String format, Object arg1, Object arg2) {
    logManager.writeLog(
        Level.ERROR, scope, pid, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  @Override
  public void error(@Nullable Marker marker, String format, Object @Nullable ... argArray) {
    logManager.writeLog(
        Level.ERROR, scope, pid, MessageFormatter.arrayFormat(format, argArray).getMessage());
  }

  @Override
  public void error(@Nullable Marker marker, String msg, @Nullable Throwable t) {
    logManager.writeLog(Level.ERROR, scope, pid, appendThrowable(msg, t));
  }

  // endregion

  // endregion Log Methods

  private static String appendThrowable(String msg, @Nullable Throwable t) {
    if (t == null) return msg;
    return msg + " " + t;
  }
}
