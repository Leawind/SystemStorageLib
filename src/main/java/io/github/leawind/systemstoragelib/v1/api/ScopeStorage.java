package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import java.util.function.Function;
import org.slf4j.Logger;

public interface ScopeStorage {

  /// Returns the scope name this storage is associated with.
  ///
  /// @return the scope name
  String name();

  /// Logger for this scope.
  ///
  /// @return the logger associated with this scope
  Logger logger();

  /// Returns the {@link StorageManager} for the given store type within this scope.
  ///
  /// @param storeType the category of storage to access
  /// @return the storage manager for this store type
  StorageManager storage(StoreType storeType);
}
