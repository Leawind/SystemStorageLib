package io.github.leawind.systemstoragelib.v1.api.managers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import org.slf4j.Logger;

public interface StorageManager {

  Logger logger();

  /// Get the storage directory path, always absolute and normalized.
  Path getDirPath();

  /// Update the storage directory path.
  ///
  /// The path is automatically converted to absolute and normalized form.
  /// Also resets the file-based lock so that the next {@link #getLock()} call creates a new lock
  /// file under the updated path. The old lock file is deleted if possible.
  void setDirPath(Path dirPath);

  /// Get the read-write lock for the storage directory.
  ///
  /// The lock file is in the directory, its name is `.lock`
  ///
  /// - Multiple readers (across processes) can hold the read lock concurrently.
  /// - The write lock is exclusive — blocks both readers and writers across all processes.
  /// - Reentrant: the same thread can acquire the same lock multiple times and must unlock equally
  /// many times.
  /// - A thread holding the read lock can acquire the read lock again but cannot upgrade to the
  /// write lock (throws {@link IllegalStateException}).
  /// - A thread holding the write lock can acquire both the read and write locks again (lock
  /// downgrade is supported).
  /// - {@code lock()} blocks until the lock is available.
  /// - {@code tryLock()} returns {@code false} if the lock cannot be acquired immediately.
  ReadWriteLock getLock() throws IOException;

  void clear() throws IOException;

  /// Delete the entire storage directory and all contents.
  void delete() throws IOException;
}
