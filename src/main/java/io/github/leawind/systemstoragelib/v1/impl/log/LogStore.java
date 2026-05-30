package io.github.leawind.systemstoragelib.v1.impl.log;

import io.github.leawind.systemstoragelib.v1.api.accessors.AbstractDirectoryAccessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public class LogStore extends AbstractDirectoryAccessor {
  private static final String LOG_FILE_NAME = "latest.log";

  private final Path logFilePath;
  private long maxFileSize;
  private int maxArchiveFiles;

  public LogStore(Path dirPath, Logger logger, long maxFileSize, int maxArchiveFiles) {
    super(dirPath, logger);
    this.logFilePath = dirPath.resolve(LOG_FILE_NAME);
    this.maxFileSize = maxFileSize;
    this.maxArchiveFiles = maxArchiveFiles;
  }

  public LogStore(Path dirPath, Logger logger) {
    this(dirPath, logger, Long.MAX_VALUE, Integer.MAX_VALUE);
  }

  public void setMaxFileSize(long value) {
    maxFileSize = value;
  }

  public void setMaxArchiveFiles(int value) {
    maxArchiveFiles = value;
  }

  /**
   * Append a log line to the shared log file.
   *
   * <p>This method acquires a cross-process write lock before writing, ensuring safety across
   * threads and processes.
   *
   * <p>IOException is silently caught and ignored to avoid affecting normal business logic.
   */
  void writeLog(long pid, Level level, String scopeName, String message) {
    try {
      rotateIfNeeded();
      String line = formatLine(pid, level, scopeName, message);
      Files.createDirectories(getDirPath());
      Files.writeString(
          logFilePath, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      getLogger().error("Failed to write log to {}", logFilePath, e);
    }
  }

  /// Rotate log files: delete `1.log` (oldest), shift `2→1, 3→2, ..., N→N-1`, then move
  /// `latest.log` to `N.log`.  Higher numbers are newer archives.
  private void rotateIfNeeded() throws IOException {
    if (!Files.exists(logFilePath) || Files.size(logFilePath) < maxFileSize) {
      return;
    }

    Files.deleteIfExists(getDirPath().resolve("1.log"));

    for (int i = 2; i <= maxArchiveFiles; i++) {
      Path src = getDirPath().resolve(i + ".log");
      Path dst = getDirPath().resolve((i - 1) + ".log");
      if (Files.exists(src)) {
        Files.move(src, dst);
      }
    }

    Files.move(logFilePath, getDirPath().resolve(maxArchiveFiles + ".log"));
  }

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private static String formatLine(long pid, Level level, String scopeName, String message) {
    // [2026-05-27T15:52:35.786Z] [3202337/Test worker] INFO () Hello world!
    return String.format(
        "[%s] [%d/%s] %s (%s) %s",
        FORMATTER.format(Instant.now()),
        pid,
        Thread.currentThread().getName(),
        level,
        scopeName,
        message);
  }
}
