package io.github.leawind.systemstoragelib.v1.api;

import dev.dirs.BaseDirectories;
import io.github.leawind.systemstoragelib.v1.impl.SystemStorageLibImpl;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SystemStorageLibHolder {
  private static final Logger LOGGER = LoggerFactory.getLogger(SystemStorageLibHolder.class);

  static final SystemStorageLib INSTANCE;

  private static SystemStorageLib bootstrap() {

    BaseDirectories baseDirs = BaseDirectories.get();
    return new SystemStorageLibImpl(
        Path.of(baseDirs.dataDir, SystemStorageLibImpl.ROOT_DIR_NAME, "logs"),
        Path.of(baseDirs.configDir, SystemStorageLibImpl.ROOT_DIR_NAME, "metaconfig"),
        Map.of(
            StoreType.CACHE,
            Path.of(
                baseDirs.cacheDir,
                SystemStorageLibImpl.ROOT_DIR_NAME,
                StoreType.CACHE.identifier()),
            StoreType.CONFIG,
            Path.of(
                baseDirs.configDir,
                SystemStorageLibImpl.ROOT_DIR_NAME,
                StoreType.CONFIG.identifier()),
            StoreType.CREDENTIALS,
            Path.of(
                baseDirs.dataDir,
                SystemStorageLibImpl.ROOT_DIR_NAME,
                StoreType.CREDENTIALS.identifier()),
            StoreType.DATA,
            Path.of(
                baseDirs.dataDir, SystemStorageLibImpl.ROOT_DIR_NAME, StoreType.DATA.identifier()),
            StoreType.DATA_LOCAL,
            Path.of(
                baseDirs.dataLocalDir,
                SystemStorageLibImpl.ROOT_DIR_NAME,
                StoreType.DATA_LOCAL.identifier())));
  }

  static {
    SystemStorageLib instance = null;
    try {
      instance = bootstrap();
    } catch (Throwable e) {
      LOGGER.error("Failed to create SystemStorageLib instance", e);
    } finally {
      INSTANCE = instance;
    }
  }
}
