package io.github.leawind.systemstoragelib.v1.api;

import dev.dirs.BaseDirectories;
import io.github.leawind.inventory.misc.Lazy;
import io.github.leawind.systemstoragelib.v1.impl.SystemStorageLibImpl;
import io.github.leawind.systemstoragelib.v1.impl.exception.SystemStorageLibException;
import java.nio.file.Path;

final class SystemStorageLibHolder {
  private static final Lazy<SystemStorageLib> instance =
      new Lazy<>(
          () -> {
            try {
              BaseDirectories baseDirs = BaseDirectories.get();
              return SystemStorageLib.builder()
                  .logsDir(Path.of(baseDirs.dataDir, SystemStorageLibImpl.ROOT_DIR_NAME, "logs"))
                  .metaConfigDir(
                      Path.of(baseDirs.configDir, SystemStorageLibImpl.ROOT_DIR_NAME, "metaconfig"))
                  .storeDir(
                      StoreType.CACHE,
                      Path.of(
                          baseDirs.cacheDir,
                          SystemStorageLibImpl.ROOT_DIR_NAME,
                          StoreType.CACHE.identifier()))
                  .storeDir(
                      StoreType.CONFIG,
                      Path.of(
                          baseDirs.configDir,
                          SystemStorageLibImpl.ROOT_DIR_NAME,
                          StoreType.CONFIG.identifier()))
                  .storeDir(
                      StoreType.CREDENTIALS,
                      Path.of(
                          baseDirs.dataDir,
                          SystemStorageLibImpl.ROOT_DIR_NAME,
                          StoreType.CREDENTIALS.identifier()))
                  .storeDir(
                      StoreType.DATA,
                      Path.of(
                          baseDirs.dataDir,
                          SystemStorageLibImpl.ROOT_DIR_NAME,
                          StoreType.DATA.identifier()))
                  .storeDir(
                      StoreType.DATA_LOCAL,
                      Path.of(
                          baseDirs.dataLocalDir,
                          SystemStorageLibImpl.ROOT_DIR_NAME,
                          StoreType.DATA_LOCAL.identifier()))
                  .build();
            } catch (SystemStorageLibException e) {
              throw e;
            } catch (Throwable e) {
              throw new SystemStorageLibException(
                  "Failed to initialize SystemStorageLib with default directories", e);
            }
          });

  static SystemStorageLib getInstance() {
    return instance.get();
  }
}
