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

  /// Returns an unmodifiable view of the custom directory mappings.
  Map<StoreType<?>, Path> customDirs();

  /// Associates a custom directory path with a store type.
  ///
  /// @param storeType the store type to configure
  /// @param path the custom directory path, must be non-null and absolute
  /// @throws NullPointerException if {@code path} is null
  /// @throws IllegalArgumentException if {@code path} is not absolute, {@code storeType} is not
  ///     customizable, or the path is already assigned to another store type
  void setCustomDir(StoreType<?> storeType, Path path);

  /// Replaces all custom directory mappings with the given entries.
  ///
  /// All entries are validated first; if any entry fails, none are applied.
  ///
  /// @param dirs map of store type to custom directory path
  /// @throws NullPointerException if {@code dirs} or any path value is null
  /// @throws IllegalArgumentException if any path is not absolute, any store type is not
  ///     customizable, or any two store types share the same path
  void setCustomDirs(Map<StoreType<?>, Path> dirs);

  /// Removes the custom directory mapping for a store type, reverting to the default.
  void unsetCustomDir(StoreType<?> storeType);

  /// Removes all custom directory mappings.
  void resetCustomDirs();

  // endregion
}
