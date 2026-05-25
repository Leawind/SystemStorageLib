package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.impl.metaconfig.PerScopeConfigImpl;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public interface PerScopeConfig {

  static PerScopeConfig createDefault() {
    return new PerScopeConfigImpl(new HashMap<>());
  }

  // region customDirs

  /// Returns the custom directory mappings for this scope.
  ///
  /// The returned map is mutable and can be modified directly. However, it differs
  /// from a regular `Map` in the following ways:
  ///
  /// - `put(StoreType, Path)` throws `IllegalArgumentException` if:
  ///   - the store type is not customizable (e.g., `StoreType.CREDENTIALS`)
  ///   - the path is not absolute
  ///   - the path is already assigned to another store type (after normalization)
  /// - `put(StoreType, Path)` throws `NullPointerException` if the path is null
  /// - `putAll(Map)` validates all entries first and applies them atomically;
  ///   if any entry fails validation, none are applied
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
