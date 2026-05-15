package io.github.leawind.systemstoragelib.v1.impl.managers;

import io.github.leawind.inventory.just.Result;
import io.github.leawind.inventory.lock.FileBasedReentrantReadWriteLock;
import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.inventory.misc.Lazy;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;

public class StorageManagerImpl implements StorageManager {
  private static final String LOCK_FILE_NAME = ".lock";

  private final Path dirPath;
  private final Path lockPath;
  private final Lazy<Result<FileBasedReentrantReadWriteLock, IOException>> lockLazy;

  public StorageManagerImpl(Path dirPath) {
    this.dirPath = dirPath;
    this.lockPath = dirPath.resolve(LOCK_FILE_NAME);
    this.lockLazy =
        new Lazy<>(
            () -> {
              try {
                Files.createDirectories(lockPath.getParent());
                return Result.ok(new FileBasedReentrantReadWriteLock(lockPath));
              } catch (IOException e) {
                return Result.err(e);
              }
            });
  }

  @Override
  public Path getDirPath() {
    return dirPath;
  }

  @Override
  public ReadWriteLock getLock() throws IOException {
    var result = lockLazy.get();
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

    try (var paths = Files.walk(dirPath)) {
      var sorted = paths.sorted(java.util.Comparator.reverseOrder()).toList();
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
    try (var unused = LockUtils.lock(getLock().writeLock())) {
      clear();
    }
    Files.deleteIfExists(lockPath);
    Files.deleteIfExists(dirPath);
  }
}
