package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CustomDirMap extends HashMap<StoreType<?>, Path> {
  @Override
  public Path put(StoreType<?> storeType, Path path)
      throws IllegalArgumentException, NullPointerException {
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
        entrySet().stream()
            .anyMatch(e -> !e.getKey().equals(storeType) && e.getValue().equals(normalized));
    if (conflict) {
      throw new IllegalArgumentException(
          "Custom directory path is already assigned to another store type: " + normalized);
    }

    return super.put(storeType, normalized);
  }

  @Override
  public void putAll(Map<? extends StoreType<?>, ? extends Path> dirs)
      throws IllegalArgumentException, NullPointerException {
    Objects.requireNonNull(dirs, "dirs must not be null");

    Map<StoreType<?>, Path> tempMap = new HashMap<>(this);
    tempMap.putAll(dirs);
    tempMap.forEach(
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

    MapUtils.requireUniqueValues(tempMap, (k, v) -> v.normalize(), "custom directory path");

    super.putAll(dirs);
  }
}
