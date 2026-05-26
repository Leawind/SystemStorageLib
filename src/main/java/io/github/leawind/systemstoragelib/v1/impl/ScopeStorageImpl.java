package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import io.github.leawind.systemstoragelib.v1.impl.managers.StorageManagerImpl;
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public final class ScopeStorageImpl implements ScopeStorage {

  private final SystemStorageLib lib;
  private final String name;
  private final Logger logger;
  private final Map<StoreType, Path> dirs;

  private final Map<StoreType, StorageManager> managers = new ConcurrentHashMap<>();

  /// ### Throws {@link IllegalArgumentException}
  ///
  /// - If any StoreType is missing.
  /// - If any dirPath is not unique.
  public ScopeStorageImpl(
      SystemStorageLib lib, String name, Logger logger, Map<StoreType, Path> dirs) {

    List<StoreType> missingTypes = StoreType.Utils.missingTypes(dirs.keySet());
    if (!missingTypes.isEmpty()) {
      throw new IllegalArgumentException("Missing StoreTypes: " + missingTypes);
    }

    MapUtils.requireUniqueValues(dirs, "dirPath");

    this.lib = lib;
    this.name = name;
    this.logger = logger;
    this.dirs = dirs;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public StorageManager storage(StoreType storeType) {
    return managers.computeIfAbsent(
        storeType, key -> new StorageManagerImpl(lib, logger, dirs.get(key).resolve(name)));
  }
}
