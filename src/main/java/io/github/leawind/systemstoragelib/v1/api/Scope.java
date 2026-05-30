package io.github.leawind.systemstoragelib.v1.api;

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

  Path directory(StoreType storeType);

  default <T extends DirectoryAccessor> T access(
      StoreType storeType, BiFunction<Path, Logger, T> factory) {
    return factory.apply(directory(storeType), logger());
  }
}
