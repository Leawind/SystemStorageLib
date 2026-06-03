package io.github.leawind.systemstoragelib.v1.impl;

import dev.dirs.BaseDirectories;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import io.github.leawind.systemstoragelib.v1.impl.exception.SystemStorageLibException;
import java.nio.file.Path;

public final class Holder {

  private static final DirectoryDocumenter DIRECTORY_DOCUMENTER;
  private static final SystemStorageLib SYSTEM_STORAGE_LIB;

  static {
    BaseDirectories baseDirs = BaseDirectories.get();

    String appName = SystemStorageLibImpl.APP_NAME;

    Path cacheRoot = Path.of(baseDirs.cacheDir, appName);
    Path configRoot = Path.of(baseDirs.configDir, appName);
    Path dataRoot = Path.of(baseDirs.dataDir, appName);
    Path dataLocalRoot = Path.of(baseDirs.dataLocalDir, appName);

    DIRECTORY_DOCUMENTER =
        DirectoryDocumenter.mutable("README.md").memorizeByResource(cacheRoot, "/readthem/root.md");

    try {
      SYSTEM_STORAGE_LIB =
          SystemStorageLib.builder(configRoot.resolve("metaconfig"))
              .logsDir(dataLocalRoot.resolve("logs"))
              .storeDir(StoreType.CACHE, cacheRoot.resolve(StoreType.CACHE.id()))
              .storeDir(StoreType.CONFIG, configRoot.resolve(StoreType.CONFIG.id()))
              .storeDir(StoreType.SECRETS, dataLocalRoot.resolve(StoreType.SECRETS.id()))
              .storeDir(StoreType.DATA, dataRoot.resolve(StoreType.DATA.id()))
              .storeDir(StoreType.DATA_LOCAL, dataLocalRoot.resolve(StoreType.DATA_LOCAL.id()))
              .build();
    } catch (SystemStorageLibException e) {
      throw e;
    } catch (Throwable e) {
      throw new SystemStorageLibException(
          "Failed to initialize SystemStorageLib with default directories", e);
    }
  }

  public static DirectoryDocumenter getDirectoryDocumenter() {
    return DIRECTORY_DOCUMENTER;
  }

  public static SystemStorageLib getSystemStorageLibInstance() {
    return SYSTEM_STORAGE_LIB;
  }
}
