package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.inventory.util.function.TriFunction;
import io.github.leawind.systemstoragelib.v1.api.managers.CredentialStore;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import io.github.leawind.systemstoragelib.v1.impl.managers.CredentialStoreImpl;
import io.github.leawind.systemstoragelib.v1.impl.managers.StorageManagerImpl;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;

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
public final class StoreType<S extends StorageManager> {

  public static final StoreType<StorageManager> CACHE =
      new StoreType<>(StorageManager.class, "cache", true, StorageManagerImpl::new);
  public static final StoreType<StorageManager> CONFIG =
      new StoreType<>(StorageManager.class, "config", true, StorageManagerImpl::new);
  public static final StoreType<CredentialStore> CREDENTIALS =
      new StoreType<>(CredentialStore.class, "credentials", false, CredentialStoreImpl::new);
  public static final StoreType<StorageManager> DATA =
      new StoreType<>(StorageManager.class, "data", true, StorageManagerImpl::new);
  public static final StoreType<StorageManager> DATA_LOCAL =
      new StoreType<>(StorageManager.class, "data_local", true, StorageManagerImpl::new);

  private static final StoreType<?>[] ALL_VALUES = {CACHE, CONFIG, CREDENTIALS, DATA, DATA_LOCAL};

  private final Class<S> clazz;
  private final String identifier;
  private final boolean allowCustomDir;
  private final TriFunction<SystemStorageLib, Logger, Path, S> managerFactory;

  private StoreType(
      Class<S> clazz,
      String identifier,
      boolean allowCustomDir,
      TriFunction<SystemStorageLib, Logger, Path, S> managerFactory) {
    this.clazz = clazz;
    this.identifier = identifier;
    this.allowCustomDir = allowCustomDir;
    this.managerFactory = managerFactory;
  }

  public Class<S> managerClass() {
    return clazz;
  }

  public S manager(SystemStorageLib lib, Logger logger, Path dirPath) {
    return managerFactory.apply(lib, logger, dirPath);
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

  public static StoreType<?>[] values() {
    return ALL_VALUES.clone();
  }

  public static StoreType<?> of(String identifier) {
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

    public static List<StoreType<?>> missingTypes(Collection<StoreType<?>> types) {
      return Arrays.stream(values()).filter(type -> !types.contains(type)).toList();
    }
  }
}
