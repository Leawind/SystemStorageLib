package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public final class ScopeStorageImpl implements ScopeStorage {
  private final String scope;
  private final Logger logger;
  private final Map<StoreType<?>, ? extends StorageManager> managers;

  /// ### Throws {@link IllegalArgumentException}
  ///
  /// - If any manager type is not the expected one for the StoreType.
  /// - If any StoreType is missing.
  /// - If any dirPath is not unique.
  public ScopeStorageImpl(
      String scope, Logger logger, Map<StoreType<?>, ? extends StorageManager> managers)
      throws IllegalArgumentException {
    validateManagers(managers);
    this.scope = scope;
    this.logger = logger;
    this.managers = managers;
  }

  private static void validateManagers(Map<StoreType<?>, ? extends StorageManager> managers)
      throws IllegalArgumentException {
    // Check manager types
    for (var entry : managers.entrySet()) {
      if (!entry.getKey().managerClass().isAssignableFrom(entry.getValue().getClass())) {
        throw new IllegalArgumentException(
            "Invalid manager type for StoreType "
                + entry.getKey()
                + ": "
                + entry.getValue().getClass().getName());
      }
    }

    // Check for missing StoreTypes.
    List<StoreType<?>> missingTypes = StoreType.Utils.missingTypes(managers.keySet());
    if (!missingTypes.isEmpty()) {
      throw new IllegalArgumentException("Missing StoreTypes: " + missingTypes);
    }

    // Check for unique manager dir path
    for (var entry : managers.entrySet()) {
      for (var other : managers.entrySet()) {
        if (!entry.getKey().equals(other.getKey())
            && entry.getValue().getDirPath().equals(other.getValue().getDirPath())) {
          throw new IllegalArgumentException(
              "dirPath for each StoreType must be unique, but "
                  + entry.getKey()
                  + " and "
                  + other.getKey()
                  + " are the same: "
                  + entry.getValue().getDirPath());
        }
      }
    }
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
