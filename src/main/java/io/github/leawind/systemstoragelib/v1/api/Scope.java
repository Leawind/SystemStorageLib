package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.inventory.util.function.TriFunction;
import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import java.nio.file.Path;
import java.util.function.Function;
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

  DirectoryDocumenter directoryDocumenter();

  default <T extends DirectoryAccessor> T access(StoreType storeType, Function<Path, T> factory) {
    return factory.apply(directory(storeType));
  }

  default <T extends DirectoryAccessor> T access(
      StoreType storeType, TriFunction<Path, Scope, DirectoryDocumenter, T> factory) {
    return factory.apply(directory(storeType), this, directoryDocumenter());
  }
}
