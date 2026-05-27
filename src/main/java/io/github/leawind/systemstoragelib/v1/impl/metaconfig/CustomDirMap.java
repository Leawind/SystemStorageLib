package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import io.github.leawind.inventory.util.ValidatingHashMap;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CustomDirMap extends ValidatingHashMap<StoreType, Path> {

  /// Validate a new entry before put it.
  ///
  /// Do not repeat what {@link #validateEntry(StoreType, Path)} does.
  ///
  /// @param normalized Normalized path
  public void validateNewEntry(StoreType storeType, Path normalized) {
    var conflicts =
        entrySet().stream()
            .filter(e -> !e.getKey().equals(storeType) & e.getValue().equals(normalized))
            .map(e -> e.getKey().id())
            .collect(Collectors.joining(", "));

    if (!conflicts.isEmpty()) {
      throw new IllegalArgumentException("Conflict custom directory path:" + conflicts);
    }
  }

  @Override
  public void validateEntry(StoreType storeType, Path path) {
    Objects.requireNonNull(path, "custom directory path must not be null");

    if (!storeType.allowCustomDir()) {
      throw new IllegalArgumentException("Store type is not customizable: " + storeType.id());
    }
    if (!path.isAbsolute()) {
      throw new IllegalArgumentException("Custom directory path must be absolute: " + path);
    }
  }

  @Override
  public void validateMap(Map<? extends StoreType, ? extends Path> m) {
    super.validateMap(m);
    MapUtils.requireUniqueValues(m, (k, v) -> v.normalize(), "custom directory path");
  }

  @Override
  public Path put(StoreType storeType, Path path) {
    Path normalized = path.normalize();
    validateNewEntry(storeType, normalized);
    return super.put(storeType, normalized);
  }

  @Override
  public void putAll(Map<? extends StoreType, ? extends Path> m) {
    var mutable = new HashMap<StoreType, Path>(m);
    mutable.replaceAll((storeType, path) -> (path).normalize());
    super.putAll(mutable);
  }
}
