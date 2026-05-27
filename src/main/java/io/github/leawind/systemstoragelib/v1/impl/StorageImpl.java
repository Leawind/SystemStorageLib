package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.inventory.event.EventEmitter;
import io.github.leawind.inventory.just.Result;
import io.github.leawind.inventory.lock.FileBasedReentrantReadWriteLock;
import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.inventory.misc.Lazy;
import io.github.leawind.inventory.misc.UncheckedCloseable;
import io.github.leawind.systemstoragelib.v1.api.Storage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class StorageImpl implements Storage {
  public static final String LOCK_FILE_NAME = ".lock";

  private final Logger logger;

  private Path dirPath;
  private Path lockPath;
  private final Lazy<Result<FileBasedReentrantReadWriteLock, IOException>> lockLazy =
      new Lazy<>(
          () -> {
            try {
              Files.createDirectories(lockPath.getParent());
              return Result.ok(new FileBasedReentrantReadWriteLock(lockPath));
            } catch (IOException e) {
              return Result.err(e);
            }
          });

  private final EventEmitter<Path> onDirUpdated = new EventEmitter<>();

  public StorageImpl(Logger logger, Path dirPath) {
    this.logger = logger;
    this.dirPath = dirPath.toAbsolutePath().normalize();
    this.lockPath = this.dirPath.resolve(LOCK_FILE_NAME);
    lockLazy.reset();
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public Path getDirPath() {
    return dirPath;
  }

  @Override
  public synchronized void setDirPath(Path dirPath) {
    try {
      Files.deleteIfExists(lockPath);
    } catch (IOException ignored) {
    }

    onDirUpdated.emit(this.dirPath);

    this.dirPath = dirPath.toAbsolutePath().normalize();
    this.lockPath = this.dirPath.resolve(LOCK_FILE_NAME);
    lockLazy.reset();
  }

  @Override
  public synchronized ReadWriteLock getLock() throws IOException {
    Result<FileBasedReentrantReadWriteLock, IOException> result = lockLazy.get();
    if (result.isOk()) {
      return result.unwrap();
    } else {
      throw result.unwrapErr();
    }
  }

  @Override
  public void clear() throws IOException {
    if (!Files.isDirectory(dirPath)) {
      return;
    }

    try (Stream<Path> paths = Files.walk(dirPath)) {
      List<Path> sorted = paths.sorted(Comparator.reverseOrder()).toList();
      for (Path path : sorted) {
        if (path.equals(dirPath) || lockPath.equals(path)) {
          continue;
        }

        Files.delete(path);
      }
    }
  }

  @Override
  public void delete() throws IOException {
    try (UncheckedCloseable ignored = LockUtils.lock(getLock().writeLock())) {
      clear();
    }
    Files.deleteIfExists(lockPath);
    Files.deleteIfExists(dirPath);
  }

  @Override
  public EventEmitter<Path> onDirUpdated() {
    return onDirUpdated;
  }
}
