package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.managers.MetaConfigManager;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.PerScopeConfig;
import io.github.leawind.systemstoragelib.v1.impl.log.LogManager;
import io.github.leawind.systemstoragelib.v1.impl.log.SystemLogger;
import io.github.leawind.systemstoragelib.v1.impl.managers.MetaConfigManagerImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SystemStorageLibImpl implements SystemStorageLib {

  public static final String ROOT_DIR_NAME = "mc_system_storage";

  private final Path logsDir;

  /// ### Examples
  ///
  /// - config: `/home/Steve/.config/mc_system_storage/config`.
  /// - data: `/home/Steve/.local/share/mc_system_storage/data`.
  private final Map<StoreType<?>, Path> defaultScopedDirs;

  private final LogManager logManager;
  private final Logger logger;
  private final MetaConfigManager metaConfig;
  private final Map<String, Optional<ScopeStorage>> scopes = new ConcurrentHashMap<>();

  /// ### Args
  ///
  /// #### `logsDir`
  ///
  /// Directory to store logs.
  ///
  /// #### `metaConfigDir`
  ///
  /// Directory to store meta configuration.
  ///
  /// #### `scopedDirs`
  ///
  /// Map of {@link StoreType} to {@link Path} of directory.
  ///
  /// - Must contain all {@link StoreType}.
  /// - Must be unique for each {@link StoreType}.
  ///
  /// Example:
  ///
  /// - config: `/home/Steve/.config/mc_system_storage/config`.
  /// - data: `/home/Steve/.local/share/mc_system_storage/data`.
  ///
  /// ### Throws {@link IllegalArgumentException}
  ///
  /// - If `scopedDirs` does not contain all {@link StoreType}.
  /// - If any value in `scopedDirs` is not unique.
  public SystemStorageLibImpl(
      Path logsDir, Path metaConfigDir, Map<StoreType<?>, Path> defaultScopedDirs) {
    validateDirs(defaultScopedDirs);

    this.logsDir = logsDir;
    this.defaultScopedDirs = new HashMap<>(defaultScopedDirs);

    logManager = new LogManager(logsDir, 10 * 1024 * 1024, 10);
    logger = new SystemLogger(logManager, "");
    metaConfig = new MetaConfigManagerImpl(logger, metaConfigDir);
    // Listen for external changes to meta config and update scope storage paths accordingly.
    metaConfig.onChanged().on(this::onUpdateMetaConfig);

    detectScopes();
  }

  private void onUpdateMetaConfig(MetaConfig newConfig) {
    // for each scope we have
    for (Map.Entry<String, Optional<ScopeStorage>> scopeEntry : scopes.entrySet()) {
      String scope = scopeEntry.getKey();
      Optional<ScopeStorage> scopeOpt = scopeEntry.getValue();
      if (scopeOpt.isEmpty()) {
        continue;
      }
      ScopeStorage scopeStorage = scopeOpt.get();

      PerScopeConfig perScopeConfig = newConfig.getScopeConfig(scope);
      Map<StoreType<?>, Path> customDirs =
          (perScopeConfig != null) ? perScopeConfig.customDirs() : null;

      // for each store type
      for (StoreType<?> storeType : StoreType.values()) {
        Path newDirPath = (customDirs != null) ? customDirs.get(storeType) : null;

        if (newDirPath == null) {
          // Not in config, use default
          newDirPath = defaultScopedDirs.get(storeType);
        }

        try {
          StorageManager manager = scopeStorage.storage(storeType);
          if (manager.getDirPath().equals(newDirPath)) {
            continue;
          }

          logger()
              .info(
                  "Updating dir path for scope `{}`, store type `{}` from `{}` to `{}`",
                  scope,
                  storeType,
                  manager.getDirPath(),
                  newDirPath.resolve(scope));

          manager.setDirPath(newDirPath.resolve(scope));
        } catch (Exception e) {
          logger.warn(
              "Failed to update dir path for scope `{}`, store type `{}`: `{}`",
              scope,
              storeType,
              e.getMessage());
        }
      }
    }
  }

  private static void validateDirs(Map<StoreType<?>, Path> scopedDirs)
      throws IllegalArgumentException {
    // Check for missing StoreTypes.
    List<StoreType<?>> missingTypes = StoreType.Utils.missingTypes(scopedDirs.keySet());
    if (!missingTypes.isEmpty()) {
      throw new IllegalArgumentException("Missing StoreTypes: " + missingTypes);
    }

    // Check for unique scopedDirs for each StoreType.
    for (Map.Entry<StoreType<?>, Path> entry : scopedDirs.entrySet()) {
      for (Map.Entry<StoreType<?>, Path> other : scopedDirs.entrySet()) {
        if (!entry.getKey().equals(other.getKey()) && entry.getValue().equals(other.getValue())) {
          throw new IllegalArgumentException(
              "dir for each StoreType must be unique, but "
                  + entry.getKey()
                  + " and "
                  + other.getKey()
                  + " are the same: "
                  + entry.getValue());
        }
      }
    }
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public MetaConfigManager metaConfig() {
    return metaConfig;
  }

  @Override
  public ScopeStorage scope(String scope) {
    validateScope(scope);

    if (scopes.containsKey(scope)) {
      Optional<ScopeStorage> optional = scopes.get(scope);
      if (optional.isPresent()) {
        return optional.get();
      }
    }

    ScopeStorage storage = createScopeStorage(scope);
    scopes.put(scope, Optional.of(storage));
    return storage;
  }

  @Override
  public Stream<String> getAllScopes() {
    detectScopes();
    return scopes.keySet().stream();
  }

  @Override
  public @Nullable String validateScope(String scope) {
    if (scope.isEmpty()) {
      return "scope is empty";
    }

    if (!(2 <= scope.length() && scope.length() <= 63)) {
      return "scope length must be between 2 and 63 characters";
    }

    char firstChar = scope.charAt(0);
    switch (firstChar) {
      case '-':
      case '+':
      case '.':
        return "scope must not start with `" + firstChar + "`";
    }

    char lastChar = scope.charAt(scope.length() - 1);
    switch (lastChar) {
      case '-':
      case '+':
      case '.':
        return "scope must not end with `" + lastChar + "`";
    }

    for (char c : scope.toCharArray()) {
      if (c >= 'A' && c <= 'Z') continue;
      if (c >= 'a' && c <= 'z') continue;
      if (c >= '0' && c <= '9') continue;
      switch (c) {
        case '_':
        case '-':
        case '+':
        case '.':
          continue;
        default:
          return "scope contains invalid characters: " + c;
      }
    }

    return null;
  }

  @Override
  public Path getLogsDir() {
    return logsDir;
  }

  private ScopeStorage createScopeStorage(String scope) {
    // Build a directory map for the given scope, preferring custom directories from MetaConfig.
    Map<StoreType<?>, Path> dirsForScope = new HashMap<>(defaultScopedDirs);
    try {
      // Load meta configuration which may contain per‑scope custom directory mappings.
      MetaConfig meta = metaConfig.get();
      PerScopeConfig perScope = meta.getScopeConfig(scope);
      if (perScope != null) {
        // Override default directories with any custom paths defined for this scope.
        perScope
            .customDirs()
            .forEach(
                (storeType, path) -> {
                  if (storeType.customizable()) {
                    dirsForScope.put(storeType, path);
                  } else {
                    logger()
                        .error("Path of store type {} is not customizable", storeType.identifier());
                  }
                });
      }
    } catch (IOException e) {
      // If we cannot read the meta config, fall back to the default scoped directories.
      logger.warn("Failed to load meta config for scope {}: {}", scope, e.getMessage());
    }
    return new ScopeStorageImpl(scope, new SystemLogger(logManager, scope), dirsForScope);
  }

  private void detectScopes() {
    scopes.values().removeIf(Optional::isEmpty);
    defaultScopedDirs.values().forEach(this::scanDirectoryForScopes);
  }

  private void scanDirectoryForScopes(Path rootDir) {
    if (!Files.isDirectory(rootDir)) {
      return;
    }
    try (Stream<Path> entries = Files.list(rootDir)) {
      entries
          .filter(Files::isDirectory)
          .map(p -> p.getFileName().toString())
          .filter(this::isScopeValid)
          .forEach(scope -> scopes.putIfAbsent(scope, Optional.empty()));
    } catch (IOException ignored) {
    }
  }
}
