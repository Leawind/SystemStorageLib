package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.PerScopeConfig;
import io.github.leawind.systemstoragelib.v1.utils.Codecs;
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PerScopeConfigImpl implements PerScopeConfig {
  public static final Codec<PerScopeConfig> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(StoreType.CODEC, Codecs.PATH)
                          .fieldOf("custom_dirs")
                          .forGetter(PerScopeConfig::customDirs))
                  .apply(inst, PerScopeConfigImpl::new));

  private final Map<StoreType<?>, Path> customDirs;

  public PerScopeConfigImpl(Map<StoreType<?>, Path> customDirs) {
    this.customDirs = new HashMap<>(customDirs);
  }

  // region customDirs

  @Override
  public Map<StoreType<?>, Path> customDirs() {
    return Collections.unmodifiableMap(customDirs);
  }

  @Override
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

  @Override
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

  @Override
  public void unsetCustomDir(StoreType<?> storeType) {
    customDirs.remove(storeType);
  }

  @Override
  public void resetCustomDirs() {
    customDirs.clear();
  }

  // endregion

  // region Object

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof PerScopeConfig other) {
      return Objects.equals(customDirs, other.customDirs());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(customDirs);
  }

  @Override
  public String toString() {
    return "PerScopeConfig[" + "customDirs=" + customDirs + ']';
  }

  // endregion
}
