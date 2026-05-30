package io.github.leawind.systemstoragelib.v1.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;

/// Defines different data types within a specific scope.
///
/// | Method | Condition |
/// | - | - |
/// | {@link #CACHE} | Renewable + cheap to regenerate |
/// | {@link #CONFIG} | Configuration files |
/// | {@link #SECRETS} | Sensitive data |
/// | {@link #DATA} | Persistent + shareable across machines |
/// | {@link #DATA_LOCAL} | Persistent + machine-local (or renewable but costly to regenerate) |
///
/// @apiNote Each category must have different directory.
public enum StoreType {
  CACHE,
  CONFIG,
  SECRETS,
  DATA,
  DATA_LOCAL;

  private final String id;

  StoreType() {
    this.id = name().toLowerCase();
  }

  public String id() {
    return id;
  }

  public static StoreType fromId(String id) {
    return valueOf(id.toUpperCase());
  }

  @ApiStatus.Internal
  public static final class Utils {
    private Utils() {}

    public static List<StoreType> missingTypes(Collection<StoreType> types) {
      return Arrays.stream(values()).filter(type -> !types.contains(type)).toList();
    }
  }
}
