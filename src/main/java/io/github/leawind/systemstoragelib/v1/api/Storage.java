package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.inventory.event.EventEmitter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import org.slf4j.Logger;

public interface Storage {

  /// Returns the logger for this storage manager.
  Logger logger();

  /// Returns the storage directory path, always absolute and normalized.
  Path getDirPath();

  /// Updates the storage directory path.
  ///
  /// The path will be converted to absolute and normalized form.
  void setDirPath(Path dirPath);

  /// Returns the read-write lock for the storage directory.
  ///
  /// The lock file is in the directory, its name is `.lock`
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
  ReadWriteLock getLock() throws IOException;

  /// Clears all contents of the storage directory.
  void clear() throws IOException;

  /// Deletes the entire storage directory and all contents.
  void delete() throws IOException;

  EventEmitter<Path> onDirUpdated();
}
