package io.github.leawind.systemstoragelib.v1.api;

import dev.dirs.BaseDirectories;
import io.github.leawind.systemstoragelib.v1.impl.SystemStorageLibImpl;
import java.nio.file.Path;

final class SystemStorageLibHolder {
  static final SystemStorageLib INSTANCE;

  static {
    var baseDirs = BaseDirectories.get();
    INSTANCE =
        new SystemStorageLibImpl(
            Path.of(baseDirs.dataDir, SystemStorageLibImpl.ROOT_DIR_NAME, "logs"),
            Path.of(baseDirs.dataDir, SystemStorageLibImpl.ROOT_DIR_NAME, "credentials"),
            Path.of(baseDirs.dataDir, SystemStorageLibImpl.ROOT_DIR_NAME, "data"),
            Path.of(baseDirs.configDir, SystemStorageLibImpl.ROOT_DIR_NAME, "config"),
            Path.of(baseDirs.cacheDir, SystemStorageLibImpl.ROOT_DIR_NAME, "cache"),
            Path.of(baseDirs.dataLocalDir, SystemStorageLibImpl.ROOT_DIR_NAME, "dataLocal"));
  }
}
