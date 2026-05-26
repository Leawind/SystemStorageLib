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
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
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

  public static final int MIN_SCOPE_LENGTH = 2;
  public static final int MAX_SCOPE_LENGTH = 128;

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
  /// #### `maxLogFileSize`
  ///
  /// Maximum size of a log file in bytes before rotation.
  ///
  /// #### `maxLogArchiveFiles`
  ///
  /// Maximum number of rotated archive log files.
  ///
  /// ### Throws {@link IllegalArgumentException}
  ///
  /// - If `scopedDirs` does not contain all {@link StoreType}.
  /// - If any value in `scopedDirs` is not unique.
  public SystemStorageLibImpl(
      Path logsDir,
      Path metaConfigDir,
      Map<StoreType<?>, Path> defaultScopedDirs,
      long maxLogFileSize,
      int maxLogArchiveFiles) {
    validateDirs(defaultScopedDirs);

    this.logsDir = logsDir;
    this.defaultScopedDirs = new HashMap<>(defaultScopedDirs);

    logManager = new LogManager(this, logsDir, maxLogFileSize, maxLogArchiveFiles);
    logger = new SystemLogger(logManager, "");
    metaConfig = new MetaConfigManagerImpl(this, logger, metaConfigDir);
    // Listen for external changes to meta config and update scope storage paths accordingly.
    metaConfig.onChanged().on(this::handleMetaConfigChanged);

    detectScopes();
  }

  private void handleMetaConfigChanged(MetaConfig newConfig) {
    // Phase 1: Compute and validate all changes across all scopes.
    Map<ScopeStorage, Map<StoreType<?>, Path>> pendingChanges = new HashMap<>();

    scopes.forEach(
        (scope, scopeOpt) -> {
          if (scopeOpt.isEmpty()) {
            return;
          }
          ScopeStorage scopeStorage = scopeOpt.get();

          PerScopeConfig perScopeConfig = newConfig.scopes().get(scope);
          Map<StoreType<?>, Path> customDirs =
              (perScopeConfig != null) ? perScopeConfig.getCustomDirs() : null;

          Map<StoreType<?>, Path> newDirMap = new HashMap<>();
          for (StoreType<?> storeType : StoreType.values()) {
            Path newDirPath = (customDirs != null) ? customDirs.get(storeType) : null;

            if (newDirPath != null && !storeType.customizable()) {
              logger()
                  .error(
                      "Path of store type {} is not customizable, ignoring custom dir in"
                          + " MetaConfig update",
                      storeType.identifier());
              continue;
            }

            if (newDirPath == null) {
              newDirPath = defaultScopedDirs.get(storeType).resolve(scope);
            }

            newDirMap.put(storeType, newDirPath);
          }

          try {
            MapUtils.requireUniqueValues(newDirMap, "dir path for each StoreType");
          } catch (IllegalArgumentException e) {
            logger()
                .warn(
                    "MetaConfig update contains duplicate dir paths for scope `{}`, ignoring: {}",
                    scope,
                    e.getMessage());
            return;
          }

          pendingChanges.put(scopeStorage, newDirMap);
        });

    // Phase 2: Apply all validated changes.
    pendingChanges.forEach(
        (scopeStorage, newDirMap) ->
            newDirMap.forEach(
                (storeType, newDirPath) -> {
                  try {
                    StorageManager manager = scopeStorage.storage(storeType);
                    if (manager.getDirPath().equals(newDirPath)) {
                      return;
                    }

                    logger()
                        .info(
                            "Updating dir path for scope `{}`, store type `{}` from `{}` to `{}`",
                            scopeStorage.name(),
                            storeType,
                            manager.getDirPath(),
                            newDirPath);

                    manager.setDirPath(newDirPath);
                  } catch (Exception e) {
                    logger()
                        .warn(
                            "Failed to update dir path for scope `{}`, store type `{}`: `{}`",
                            scopeStorage.name(),
                            storeType,
                            e.getMessage());
                  }
                }));
  }

  private static void validateDirs(Map<StoreType<?>, Path> scopedDirs)
      throws IllegalArgumentException {
    // Check for missing StoreTypes.
    List<StoreType<?>> missingTypes = StoreType.Utils.missingTypes(scopedDirs.keySet());
    if (!missingTypes.isEmpty()) {
      throw new IllegalArgumentException("Missing StoreTypes: " + missingTypes);
    }

    MapUtils.requireUniqueValues(scopedDirs, "dir for each StoreType");
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
  public ScopeStorage scope(String scopeName) {
    String err = validateScopeName(scopeName);
    if (err != null) {
      throw new IllegalArgumentException("Invalid scope: " + err);
    }

    return scopes
        .compute(
            scopeName,
            (k, v) -> {
              if (v != null && v.isPresent()) {
                return v;
              }
              return Optional.of(createScopeStorage(k));
            })
        .get();
  }

  @Override
  public Stream<String> streamScopes() {
    detectScopes();
    return scopes.keySet().stream();
  }

  @Override
  public @Nullable String validateScopeName(String scopeName) {
    if (scopeName.isEmpty()) {
      return "scope is empty";
    }

    if (!(MIN_SCOPE_LENGTH <= scopeName.length() && scopeName.length() <= MAX_SCOPE_LENGTH)) {
      return "scope length must be between "
          + MIN_SCOPE_LENGTH
          + " and "
          + MAX_SCOPE_LENGTH
          + " characters";
    }

    char firstChar = scopeName.charAt(0);
    switch (firstChar) {
      case '-':
      case '+':
      case '.':
        return "scope must not start with `" + firstChar + "`";
    }

    char lastChar = scopeName.charAt(scopeName.length() - 1);
    switch (lastChar) {
      case '-':
      case '+':
      case '.':
        return "scope must not end with `" + lastChar + "`";
    }

    for (char c : scopeName.toCharArray()) {
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

  private ScopeStorage createScopeStorage(String scopeName) {
    // Build a directory map for the given scope, preferring custom directories from MetaConfig.
    Map<StoreType<?>, Path> dirsForScope = new HashMap<>(defaultScopedDirs);
    try {
      // Load meta configuration which may contain per‑scope custom directory mappings.
      MetaConfig meta = metaConfig.get();
      PerScopeConfig perScope = meta.scopes().get(scopeName);
      if (perScope != null) {
        // Override default directories with any custom paths defined for this scope.
        perScope
            .getCustomDirs()
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
      logger().warn("Failed to load meta config for scope {}: {}", scopeName, e.getMessage());
    }
    return new ScopeStorageImpl(
        this, scopeName, new SystemLogger(logManager, scopeName), dirsForScope);
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
          .map(scopeDirPath -> scopeDirPath.getFileName().toString())
          .filter(scopeName -> validateScopeName(scopeName) == null)
          .forEach(scopeName -> scopes.putIfAbsent(scopeName, Optional.empty()));
    } catch (IOException ignored) {
    }
  }
}
