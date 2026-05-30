package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.accessors.SecretsAccessor;
import java.nio.file.Path;
import java.util.function.BiFunction;
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

  /// Returns the storage directory path for the given store type.
  ///
  /// @param storeType the store type
  Path directory(StoreType storeType);

  /// ### Example
  ///
  /// ```java
  /// SecretsAccessor secrets = scope.access(StoreType.SECRETS, SecretsAccessor::from)
  /// ```
  ///
  /// @see SecretsAccessor
  default <T extends DirectoryAccessor> T access(
      StoreType storeType, BiFunction<Path, Logger, T> factory) {
    return factory.apply(directory(storeType), logger());
  }
}
