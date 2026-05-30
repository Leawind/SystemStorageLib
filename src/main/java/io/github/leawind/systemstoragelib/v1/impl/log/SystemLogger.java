package io.github.leawind.systemstoragelib.v1.impl.log;

import io.github.leawind.systemstoragelib.v1.utils.ForwardingLogger;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

public class SystemLogger extends ForwardingLogger {
  private static final Logger SLF4J_LOGGER = LoggerFactory.getLogger(SystemLogger.class);

  private final LogAccessor logAccessor;
  private final long pid;

  public SystemLogger(LogAccessor logAccessor, String scopeName) {
    super(SLF4J_LOGGER);
    this.name = scopeName;
    this.logAccessor = Objects.requireNonNull(logAccessor);
    this.pid = ProcessHandle.current().pid();
  }

  @Override
  protected String getFullyQualifiedCallerName() {
    return name;
  }

  @Override
  protected void handleNormalizedLoggingCall(
      Level level,
      Marker marker,
      String messagePattern,
      Object[] arguments,
      @Nullable Throwable throwable) {
    super.handleNormalizedLoggingCall(
        level, marker, "(" + name + ") " + messagePattern, arguments, throwable);
    logAccessor.writeLog(
        pid,
        level,
        name,
        MessageFormatter.arrayFormat(messagePattern, arguments, throwable).getMessage());
  }
}
