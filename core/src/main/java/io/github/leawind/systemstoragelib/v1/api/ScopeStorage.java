package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.inventory.type.UnsafeTypeUtils;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import io.github.leawind.systemstoragelib.v1.impl.ScopeStorageImpl;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

public interface ScopeStorage {
  String scope();

  Logger logger();

  <S extends StorageManager> S storage(StoreType<S> storeType);

  static ScopeStorage ofDirs(String scope, Logger logger, Map<StoreType<?>, Path> dirs)
      throws IllegalArgumentException {
    Map<StoreType<?>, ? extends StorageManager> managers = new HashMap<>();
    for (var entry : dirs.entrySet()) {
      StoreType<?> type = entry.getKey();
      Path scopedPath = entry.getValue().resolve(scope);

      managers.put(type, UnsafeTypeUtils.cast(type.manager(scopedPath)));
    }
    return of(scope, logger, managers);
  }

  static ScopeStorage of(
      String scope, Logger logger, Map<StoreType<?>, ? extends StorageManager> managers)
      throws IllegalArgumentException {
    return new ScopeStorageImpl(scope, logger, managers);
  }
}
