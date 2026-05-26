package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/// Defines storage locations for different types of data within a specific scope.
/// Each scope provides access to storage managers tailored for different data categories.
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
public final class StoreType {

  public static final StoreType CACHE = new StoreType("cache", true);
  public static final StoreType CONFIG = new StoreType("config", true);
  public static final StoreType CREDENTIALS = new StoreType("credentials", false);
  public static final StoreType DATA = new StoreType("data", true);
  public static final StoreType DATA_LOCAL = new StoreType("data_local", true);

  private static final StoreType[] ALL_VALUES = {CACHE, CONFIG, CREDENTIALS, DATA, DATA_LOCAL};

  private final String identifier;
  private final boolean allowCustomDir;

  private StoreType(String identifier, boolean allowCustomDir) {
    this.identifier = identifier;
    this.allowCustomDir = allowCustomDir;
  }

  public String identifier() {
    return identifier;
  }

  /// @see ScopeMetaConfig#getCustomDirs()
  public boolean allowCustomDir() {
    return allowCustomDir;
  }

  @Override
  public String toString() {
    return identifier;
  }

  public static StoreType[] values() {
    return ALL_VALUES.clone();
  }

  public static StoreType of(String identifier) {
    return switch (identifier) {
      case "cache" -> CACHE;
      case "config" -> CONFIG;
      case "credentials" -> CREDENTIALS;
      case "data" -> DATA;
      case "data_local" -> DATA_LOCAL;
      default -> throw new IllegalArgumentException("Unknown store type: " + identifier);
    };
  }

  public static final class Utils {
    private Utils() {}

    public static List<StoreType> missingTypes(Collection<StoreType> types) {
      return Arrays.stream(values()).filter(type -> !types.contains(type)).toList();
    }
  }
}
