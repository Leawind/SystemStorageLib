package io.github.leawind.systemstoragelib.v1.api;

import com.mojang.serialization.Codec;
import io.github.leawind.systemstoragelib.v1.api.managers.CredentialStore;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import io.github.leawind.systemstoragelib.v1.impl.managers.CredentialStoreImpl;
import io.github.leawind.systemstoragelib.v1.impl.managers.StorageManagerImpl;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import org.slf4j.Logger;

/// Defines storage locations for different types of data within a specific scope.
/// Each scope provides access to storage managers tailored for different data categories.
///
/// | Method | Condition |
/// | - | - |
/// | {@link #CREDENTIALS} | Sensitive credentials |
/// | {@link #CONFIG} | Configuration files |
/// | {@link #DATA} | Persistent + shareable across machines |
/// | {@link #CACHE} | Persistent + machine-local (or renewable but costly to regenerate) |
/// | {@link #DATA_LOCAL} | Renewable + cheap to regenerate |
///
/// @apiNote Each category must have different directory.
public final class StoreType<S extends StorageManager> {

  public static final Codec<StoreType<?>> CODEC =
      Codec.STRING.xmap(StoreType::of, StoreType::identifier);

  public static final StoreType<CredentialStore> CREDENTIALS =
      new StoreType<>(CredentialStore.class, "credentials", false, CredentialStoreImpl::new);
  public static final StoreType<StorageManager> CONFIG =
      new StoreType<>(StorageManager.class, "config", true, StorageManagerImpl::new);
  public static final StoreType<StorageManager> DATA =
      new StoreType<>(StorageManager.class, "data", true, StorageManagerImpl::new);
  public static final StoreType<StorageManager> CACHE =
      new StoreType<>(StorageManager.class, "cache", true, StorageManagerImpl::new);
  public static final StoreType<StorageManager> DATA_LOCAL =
      new StoreType<>(StorageManager.class, "data_local", true, StorageManagerImpl::new);

  private final Class<S> clazz;
  private final String identifier;
  private final boolean customizable;
  private final BiFunction<Logger, Path, S> managerFactory;

  private StoreType(
      Class<S> clazz,
      String identifier,
      boolean customizable,
      BiFunction<Logger, Path, S> managerFactory) {
    this.clazz = clazz;
    this.identifier = identifier;
    this.customizable = customizable;
    this.managerFactory = managerFactory;
  }

  public Class<S> managerClass() {
    return clazz;
  }

  public S manager(Logger logger, Path dirPath) {
    return managerFactory.apply(logger, dirPath);
  }

  public String identifier() {
    return identifier;
  }

  public boolean customizable() {
    return customizable;
  }

  public static StoreType<?>[] values() {
    return new StoreType<?>[] {CREDENTIALS, CONFIG, DATA, CACHE, DATA_LOCAL};
  }

  public static StoreType<?> of(String identifier) {
    switch (identifier) {
      case "credentials":
        return CREDENTIALS;
      case "config":
        return CONFIG;
      case "data":
        return DATA;
      case "cache":
        return CACHE;
      case "data_local":
        return DATA_LOCAL;
      default:
        throw new IllegalArgumentException("Unknown store type: " + identifier);
    }
  }

  public static final class Utils {
    private Utils() {}

    public static List<StoreType<?>> missingTypes(Collection<StoreType<?>> types) {
      return Arrays.stream(values()).filter(type -> !types.contains(type)).toList();
    }
  }
}
