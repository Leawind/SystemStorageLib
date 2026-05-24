package io.github.leawind.systemstoragelib.v1.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class MapUtils {
  private MapUtils() {}

  /// Validates that all derived values in the map are unique.
  ///
  /// If a duplicate is found, throws {@link IllegalArgumentException} with a message naming both
  /// conflicting keys.
  ///
  /// @param <K> the key type
  /// @param <V> the value type
  /// @param <T> the type of the derived value to compare
  /// @param map the map to validate
  /// @param valueMapper a function to extract the comparison value from each entry
  /// @param valueDescription a human-readable description of the value being compared, used in the
  ///     error message
  /// @throws IllegalArgumentException if any two entries produce the same derived value
  public static <K, V, T> void requireUniqueValues(
      Map<K, V> map, BiFunction<K, V, T> valueMapper, String valueDescription) {
    Map<T, K> seen = new HashMap<>();
    for (Map.Entry<K, V> entry : map.entrySet()) {
      T value = valueMapper.apply(entry.getKey(), entry.getValue());
      K previous = seen.putIfAbsent(value, entry.getKey());
      if (previous != null) {
        throw new IllegalArgumentException(
            valueDescription
                + " must be unique, but "
                + previous
                + " and "
                + entry.getKey()
                + " are the same: "
                + value);
      }
    }
  }

  public static <K, V> void requireUniqueValues(Map<K, V> map, String valueDescription) {
    requireUniqueValues(map, (k, v) -> v, valueDescription);
  }
}
