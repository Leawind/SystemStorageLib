package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import io.github.leawind.systemstoragelib.v1.api.StoreType;
import java.nio.file.Path;
import java.util.Map;

public interface ScopeMetaConfig {

  // region customDirs

  /// Returns the custom directory mappings for this scope.
  ///
  /// The returned map is mutable and can be modified directly.
  ///
  /// However, it differs from a regular `Map`, it validates all entries atomically.
  ///
  /// - the store type must be customizable ({@link StoreType#allowCustomDir()})
  /// - the path must be absolute ({@link Path#isAbsolute()})
  /// - the path must be unique in the map
  ///
  /// - Paths are normalized before being stored
  ///
  /// @return the custom directory mappings
  Map<StoreType<?>, Path> getCustomDirs();

  /// Replaces all custom directory mappings with the given entries.
  ///
  /// All entries are validated first; if any entry fails, none are applied.
  ///
  /// @param dirs map of store type to custom directory path
  /// @throws NullPointerException if {@code dirs} or any path value is null
  /// @throws IllegalArgumentException if any path is not absolute, any store type is not
  ///     customizable, or any two store types share the same path
  void setCustomDirs(Map<StoreType<?>, Path> dirs);

  // endregion
}
