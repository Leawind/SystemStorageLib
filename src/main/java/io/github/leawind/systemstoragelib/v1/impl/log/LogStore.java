package io.github.leawind.systemstoragelib.v1.impl.log;

import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.inventory.misc.UncheckedCloseable;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.impl.StorageImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class LogStore extends StorageImpl implements AutoCloseable {
  private static final String LOG_FILE_NAME = "latest.log";

  private final Path logFilePath;
  private final long maxFileSize;
  private final int maxArchiveFiles;

  public LogStore(SystemStorageLib lib, Path logDir, long maxFileSize, int maxArchiveFiles) {
    super(lib, LoggerFactory.getLogger(LogStore.class), logDir);
    this.logFilePath = logDir.resolve(LOG_FILE_NAME);
    this.maxFileSize = maxFileSize;
    this.maxArchiveFiles = maxArchiveFiles;
  }

  /**
   * Append a log line to the shared log file.
   *
   * <p>This method acquires a cross-process write lock before writing, ensuring safety across
   * threads and processes.
   *
   * <p>IOException is silently caught and ignored to avoid affecting normal business logic.
   */
  public void writeLog(Level level, String scopeName, long pid, String message) {
    try {
      try (UncheckedCloseable ignored = LockUtils.lock(getLock().writeLock())) {
        rotateIfNeeded();
        String line = formatLine(level, scopeName, pid, message);
        Files.writeString(
            logFilePath, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      }
    } catch (IOException e) {
      logger().error("Failed to write log to {}", logFilePath, e);
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

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(getDirPath().resolve(LOCK_FILE_NAME));
  }

  private static String formatLine(Level level, String scopeName, long pid, String message) {
    return String.format(
        "[%s] [%s] [%d] [%s] %s", Instant.now().toString(), level, pid, scopeName, message);
  }
}
