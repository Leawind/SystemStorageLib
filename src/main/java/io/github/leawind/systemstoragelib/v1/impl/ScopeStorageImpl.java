package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.inventory.type.UnsafeTypeUtils;
import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public final class ScopeStorageImpl implements ScopeStorage {
  private final String scope;
  private final Logger logger;
  private final Map<StoreType<?>, ? extends StorageManager> managers;

  /// ### Throws {@link IllegalArgumentException}
  ///
  /// - If any StoreType is missing.
  /// - If any dirPath is not unique.
  public ScopeStorageImpl(String scope, Logger logger, Map<StoreType<?>, Path> dirs) {
    Map<StoreType<?>, ? extends StorageManager> managers = new HashMap<>();

    for (Map.Entry<StoreType<?>, Path> entry : dirs.entrySet()) {
      StoreType<?> type = entry.getKey();
      Path scopedPath = entry.getValue().resolve(scope);

      managers.put(type, UnsafeTypeUtils.cast(type.manager(logger, scopedPath)));
    }
    validateManagers(managers);

    this.scope = scope;
    this.logger = logger;
    this.managers = managers;
  }

  private static void validateManagers(Map<StoreType<?>, ? extends StorageManager> managers)
      throws IllegalArgumentException {
    // Check for missing StoreTypes.
    List<StoreType<?>> missingTypes = StoreType.Utils.missingTypes(managers.keySet());
    if (!missingTypes.isEmpty()) {
      throw new IllegalArgumentException("Missing StoreTypes: " + missingTypes);
    }

    MapUtils.requireUniqueValues(managers, (k, v) -> v.getDirPath(), "dirPath for each StoreType");
  }

  @Override
  public String scope() {
    return scope;
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends StorageManager> S storage(StoreType<S> storeType) {
    return (S) managers.get(storeType);
  }
}
