package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/// Defines different data types within a specific scope.
///
/// | Method | Condition |
/// | - | - |
/// | {@link #CACHE} | Renewable + cheap to regenerate |
/// | {@link #CONFIG} | Configuration files |
/// | {@link #CREDENTIALS} | Sensitive credentials |
/// | {@link #DATA} | Persistent + shareable across machines |
/// | {@link #DATA_LOCAL} | Persistent + machine-local (or renewable but costly to regenerate) |
///
/// @apiNote Each category must have different directory.
public enum StoreType {
  CACHE("cache", true),
  CONFIG("config", true),
  CREDENTIALS("credentials", false),
  DATA("data", true),
  DATA_LOCAL("data_local", true);

  private final String id;
  private final boolean allowCustomDir;

  StoreType(String id, boolean allowCustomDir) {
    this.id = id;
    this.allowCustomDir = allowCustomDir;
  }

  public String id() {
    return id;
  }

  /// @see ScopeMetaConfig#getCustomDirs()
  public boolean allowCustomDir() {
    return allowCustomDir;
  }

  public static StoreType fromId(String id) {
    return switch (id) {
      case "cache" -> CACHE;
      case "config" -> CONFIG;
      case "credentials" -> CREDENTIALS;
      case "data" -> DATA;
      case "data_local" -> DATA_LOCAL;
      default -> throw new IllegalArgumentException("Unknown store type: " + id);
    };
  }

  public static final class Utils {
    private Utils() {}

    public static List<StoreType> missingTypes(Collection<StoreType> types) {
      return Arrays.stream(values()).filter(type -> !types.contains(type)).toList();
    }
  }
}
