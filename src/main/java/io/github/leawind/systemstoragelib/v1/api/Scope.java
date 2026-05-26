package io.github.leawind.systemstoragelib.v1.api;

import org.slf4j.Logger;

public interface Scope {

  /// Returns the scope name this storage is associated with.
  ///
  /// @return the scope name
  String name();

  /// Logger for this scope.
  ///
  /// @return the logger associated with this scope
  Logger logger();

  /// Returns the {@link Storage} for the given store type within this scope.
  ///
  /// @param storeType the category of storage to access
  /// @return the storage manager for this store type
  Storage storage(StoreType storeType);
}
