package io.github.leawind.systemstoragelib.v1.utils;

import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;

public abstract class ForwardingLogger extends AbstractLogger {
  protected Logger logger;

  protected ForwardingLogger(Logger logger) {
    this.logger = logger;
  }

  @Override
  protected void handleNormalizedLoggingCall(
      Level level,
      Marker marker,
      String messagePattern,
      Object[] arguments,
      @Nullable Throwable throwable) {
    Object[] args = arguments;
    if (throwable != null) {
      if (args == null) {
        args = new Object[] {throwable};
      } else {
        args = Arrays.copyOf(args, args.length + 1);
        args[args.length - 1] = throwable;
      }
    }

    switch (level) {
      case ERROR -> logger.error(marker, messagePattern, args);
      case WARN -> logger.warn(marker, messagePattern, args);
      case INFO -> logger.info(marker, messagePattern, args);
      case DEBUG -> logger.debug(marker, messagePattern, args);
      case TRACE -> logger.trace(marker, messagePattern, args);
    }
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return logger.isTraceEnabled(marker);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return logger.isDebugEnabled(marker);
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return logger.isInfoEnabled(marker);
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return logger.isWarnEnabled(marker);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return logger.isErrorEnabled(marker);
  }
}
