package io.github.leawind.systemstoragelib.v1.api.accessors;

import io.github.leawind.inventory.event.EventEmitter;
import io.github.leawind.inventory.lock.FileBasedReentrantReadWriteLock;
import io.github.leawind.systemstoragelib.v1.api.DirectoryAccessor;
import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import io.github.leawind.systemstoragelib.v1.impl.Holder;
import java.io.IOException;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

public class AbstractDirectoryAccessor implements DirectoryAccessor {
  private Path dirPath;
  private Logger logger;
  protected final DirectoryDocumenter directoryDocumenter;
  private final EventEmitter<Path> onDirPathChanged = new EventEmitter<>();

  protected AbstractDirectoryAccessor(
      Path dirPath, Logger logger, DirectoryDocumenter directoryDocumenter) {
    this.dirPath = dirPath.normalize();
    this.logger = logger;
    this.directoryDocumenter = directoryDocumenter;
  }

  @Override
  public Path getDirPath() {
    return dirPath;
  }

  @Override
  public void setDirPath(Path newPath) {
    newPath = newPath.normalize();
    if (this.dirPath.equals(newPath)) {
      return;
    }
    this.dirPath = newPath;
    onDirPathChanged.emit(newPath);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public void setLogger(@NonNull Logger logger) {
    this.logger = logger;
  }

  @Override
  public EventEmitter<Path> onDirPathChanged() {
    return onDirPathChanged;
  }

  /// Create a file-based lock with given file path.
  ///
  /// - Multiple readers (across processes) can hold the read lock concurrently.
  /// - The write lock is exclusive — blocks both readers and writers across all processes.
  /// - Reentrant: the same thread can acquire the same lock multiple times and must unlock equally
  ///   many times.
  /// - A thread holding the read lock can acquire the read lock again but cannot upgrade to the
  ///   write lock (throws `IllegalStateException`).
  /// - A thread holding the write lock can acquire both the read and write locks again (lock
  ///   downgrade is supported).
  /// - `lock()` blocks until the lock is available.
  /// - `tryLock()` returns `false` if the lock cannot be acquired immediately.
  public static FileBasedReentrantReadWriteLock createLock(Path lockFilePath) {
    try {
      Holder.getDirectoryDocumenter().createDirectories(lockFilePath.getParent());
      return new FileBasedReentrantReadWriteLock(lockFilePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
