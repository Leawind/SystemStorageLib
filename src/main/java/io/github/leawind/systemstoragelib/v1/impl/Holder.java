package io.github.leawind.systemstoragelib.v1.impl;

import dev.dirs.BaseDirectories;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import io.github.leawind.systemstoragelib.v1.impl.exception.SystemStorageLibException;
import java.nio.file.Path;

public final class Holder {

  public static final DirectoryDocumenter DIRECTORY_DOCUMENTER;
  public static final SystemStorageLib SYSTEM_STORAGE_LIB;

  static {
    BaseDirectories baseDirs = BaseDirectories.get();

    String appName = SystemStorageLibImpl.APP_NAME;

    Path cacheDir = Path.of(baseDirs.cacheDir, appName);
    Path configDir = Path.of(baseDirs.configDir, appName);
    Path dataDir = Path.of(baseDirs.dataDir, appName);
    Path dataLocalDir = Path.of(baseDirs.dataLocalDir, appName);

    DIRECTORY_DOCUMENTER =
        DirectoryDocumenter.create("README.md").memorizeByResource(cacheDir, "/readthem/root.md");

    try {
      SYSTEM_STORAGE_LIB =
          SystemStorageLib.builder(configDir.resolve("metaconfig"))
              .logsDir(dataLocalDir.resolve("logs"))
              .storeDir(StoreType.CACHE, cacheDir.resolve(StoreType.CACHE.id()))
              .storeDir(StoreType.CONFIG, configDir.resolve(StoreType.CONFIG.id()))
              .storeDir(StoreType.SECRETS, dataLocalDir.resolve(StoreType.SECRETS.id()))
              .storeDir(StoreType.DATA, dataDir.resolve(StoreType.DATA.id()))
              .storeDir(StoreType.DATA_LOCAL, dataLocalDir.resolve(StoreType.DATA_LOCAL.id()))
              .build();
    } catch (SystemStorageLibException e) {
      throw e;
    } catch (Throwable e) {
      throw new SystemStorageLibException(
          "Failed to initialize SystemStorageLib with default directories", e);
    }
  }
}
