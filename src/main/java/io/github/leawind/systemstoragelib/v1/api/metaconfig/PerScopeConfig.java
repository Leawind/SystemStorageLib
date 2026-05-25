package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.utils.Codecs;
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PerScopeConfig {
  public static final Codec<PerScopeConfig> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(StoreType.CODEC, Codecs.PATH)
                          .fieldOf("custom_dirs")
                          .forGetter(PerScopeConfig::customDirs))
                  .apply(inst, PerScopeConfig::new));
  private final Map<StoreType<?>, Path> customDirs;

  PerScopeConfig(Map<StoreType<?>, Path> customDirs) {
    this.customDirs = new HashMap<>(customDirs);
  }

  public static PerScopeConfig getDefault() {
    return new PerScopeConfig(new HashMap<>());
  }

  // region customDirs

  /// Returns an unmodifiable view of the custom directory mappings.
  public Map<StoreType<?>, Path> customDirs() {
    return Collections.unmodifiableMap(customDirs);
  }

  /// Associates a custom directory path with a store type.
  ///
  /// @param storeType the store type to configure
  /// @param path the custom directory path, must be non-null and absolute
  /// @throws NullPointerException if {@code path} is null
  /// @throws IllegalArgumentException if {@code path} is not absolute, {@code storeType} is not
  ///     customizable, or the path is already assigned to another store type
  public void setCustomDir(StoreType<?> storeType, Path path) {
    Objects.requireNonNull(path, "custom directory path must not be null");
    if (!storeType.customizable()) {
      throw new IllegalArgumentException(
          "Store type is not customizable: " + storeType.identifier());
    }
    if (!path.isAbsolute()) {
      throw new IllegalArgumentException("Custom directory path must be absolute: " + path);
    }
    Path normalized = path.normalize();
    boolean conflict =
        customDirs.entrySet().stream()
            .anyMatch(e -> !e.getKey().equals(storeType) && e.getValue().equals(normalized));
    if (conflict) {
      throw new IllegalArgumentException(
          "Custom directory path is already assigned to another store type: " + normalized);
    }
    customDirs.put(storeType, normalized);
  }

  /// Replaces all custom directory mappings with the given entries.
  ///
  /// All entries are validated first; if any entry fails, none are applied.
  ///
  /// @param dirs map of store type to custom directory path
  /// @throws NullPointerException if {@code dirs} or any path value is null
  /// @throws IllegalArgumentException if any path is not absolute, any store type is not
  ///     customizable, or any two store types share the same path
  public void setCustomDirs(Map<StoreType<?>, Path> dirs) {
    Objects.requireNonNull(dirs, "dirs must not be null");
    // Validate all entries first
    dirs.forEach(
        (storeType, path) -> {
          Objects.requireNonNull(path, "custom directory path must not be null");
          if (!storeType.customizable()) {
            throw new IllegalArgumentException(
                "Store type is not customizable: " + storeType.identifier());
          }
          if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Custom directory path must be absolute: " + path);
          }
        });

    MapUtils.requireUniqueValues(dirs, (k, v) -> v.normalize(), "custom directory path");

    customDirs.clear();
    dirs.forEach((storeType, path) -> customDirs.put(storeType, path.normalize()));
  }

  /// Removes the custom directory mapping for a store type, reverting to the default.
  public void unsetCustomDir(StoreType<?> storeType) {
    customDirs.remove(storeType);
  }

  /// Removes all custom directory mappings.
  public void resetCustomDirs() {
    customDirs.clear();
  }

  // endregion

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PerScopeConfig)) {
      return false;
    }
    return Objects.equals(customDirs, ((PerScopeConfig) o).customDirs);
  }

  @Override
  public String toString() {
    return "PerScopeConfig[" + "customDirs=" + customDirs + ']';
  }
}
