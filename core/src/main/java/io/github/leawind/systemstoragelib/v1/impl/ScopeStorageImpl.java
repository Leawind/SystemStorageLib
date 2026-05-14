package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.managers.CredentialStore;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;

public record ScopeStorageImpl(
    String scope,
    Logger logger,
    CredentialStore credentials,
    StorageManager data,
    StorageManager config,
    StorageManager cache,
    StorageManager dataLocal)
    implements ScopeStorage {
  public ScopeStorageImpl {
    assertNoConflict(credentials, data, config, cache, dataLocal);
  }

  public static void assertNoConflict(StorageManager... storageManagers)
      throws IllegalArgumentException {
    Set<Path> dirSet = new HashSet<>();

    for (var storage : storageManagers) {
      var dir = storage.getDirPath();
      if (dirSet.contains(dir)) {
        throw new IllegalArgumentException("Conflict directory: " + dir);
      }
      dirSet.add(dir);
    }
  }
}
