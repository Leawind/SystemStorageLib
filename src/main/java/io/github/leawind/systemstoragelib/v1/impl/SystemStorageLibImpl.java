package io.github.leawind.systemstoragelib.v1.impl;

import dev.dirs.BaseDirectories;
import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.Storage;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import io.github.leawind.systemstoragelib.v1.api.stores.MetaConfigManager;
import io.github.leawind.systemstoragelib.v1.impl.log.LogManager;
import io.github.leawind.systemstoragelib.v1.impl.log.SystemLogger;
import io.github.leawind.systemstoragelib.v1.impl.stores.MetaConfigManagerImpl;
import io.github.leawind.systemstoragelib.v1.utils.ConcurrentScopeHashMap;
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SystemStorageLibImpl implements SystemStorageLib {

  public static final String ROOT_DIR_NAME = "mc_system_storage";

  public static final int MIN_SCOPE_NAME_LENGTH = 2;
  public static final int MAX_SCOPE_NAME_LENGTH = 128;

  private final Path logsDir;

  /// ### Examples
  ///
  /// - config: `/home/Steve/.config/<root_name>/config`.
  /// - data: `/home/Steve/.local/share/<root_name>/data`.
  private final Map<StoreType, Path> defaultScopedDirs;

  private final LogManager logManager;
  private final Logger logger;
  private final MetaConfigManager metaConfig;
  private final Map<String, Optional<Scope>> scopes;

  /// ## Args
  ///
  /// All paths will be converted to absolute and normalized.
  ///
  /// - `logsDir` Directory to store logs.
  /// - `metaConfigDir` Directory to store meta configuration.
  /// - `defaultScopedDirs` Map from {@link StoreType} to default directory.
  ///   - Must contain all {@link StoreType}.
  ///   - Path must be unique.
  ///
  ///   Example:
  ///
  ///   - config: `/home/Steve/.config/<root_name>/config`.
  ///   - data: `/home/Steve/.local/share/<root_name>/data`.
  /// - `maxLogFileSize` Maximum size of a log file in bytes before rotation.
  /// - `maxLogArchiveFiles` Maximum number of rotated archive log files.
  ///
  /// ## Throws
  ///
  /// - {@link IllegalArgumentException} if:
  ///   - `maxLogFileSize` <= 1
  ///   - `maxLogArchiveFiles` <= 1
  ///   - any of logs`logsDir`, `metaConfigDir`, or value in `defaultScopedDirs` is null.
  ///   - `defaultScopedDirs` does not contain all {@link StoreType}.
  ///   - any directory path is not unique.
  public SystemStorageLibImpl(
      @NonNull Path logsDir,
      @NonNull Path metaConfigDir,
      @NonNull Map<StoreType, Path> defaultScopedDirs,
      long maxLogFileSize,
      int maxLogArchiveFiles) {

    if (maxLogFileSize <= 1) {
      throw new IllegalArgumentException("maxLogFileSize is too small: " + maxLogFileSize);
    }
    if (maxLogArchiveFiles <= 1) {
      throw new IllegalArgumentException("maxLogArchiveFiles is too small: " + maxLogArchiveFiles);
    }

    Objects.requireNonNull(logsDir);
    Objects.requireNonNull(metaConfigDir);

    logsDir = logsDir.toAbsolutePath().normalize();
    metaConfigDir = metaConfigDir.toAbsolutePath().normalize();

    validateDirs(defaultScopedDirs);

    if (defaultScopedDirs.containsValue(logsDir)) {
      throw new IllegalArgumentException("logsDir is already used: " + logsDir);
    }
    if (defaultScopedDirs.containsValue(metaConfigDir)) {
      throw new IllegalArgumentException("metaConfigDir is already used: " + metaConfigDir);
    }

    this.logsDir = logsDir;
    this.defaultScopedDirs = new HashMap<>(defaultScopedDirs);

    logManager = new LogManager(this, logsDir, maxLogFileSize, maxLogArchiveFiles);
    logger = new SystemLogger(logManager, "");
    metaConfig = new MetaConfigManagerImpl(this, logger, metaConfigDir);
    // Listen for external changes to meta config and update scope storage paths accordingly.
    metaConfig.onChanged().on(this::handleMetaConfigChanged);

    scopes = new ConcurrentScopeHashMap<>(this);

    detectScopes();
  }

  private void handleMetaConfigChanged(MetaConfig newConfig) {
    // Phase 1: Compute and validate all changes across all scopes.
    Map<Scope, Map<StoreType, Path>> pendingChanges = new HashMap<>();

    scopes.forEach(
        (scopeName, scopeOpt) -> {
          if (scopeOpt.isEmpty()) {
            return;
          }
          Scope scope = scopeOpt.get();

          ScopeMetaConfig scopeMetaConfig = newConfig.scopes().get(scopeName);
          Map<StoreType, Path> customDirs =
              (scopeMetaConfig != null) ? scopeMetaConfig.getCustomDirs() : null;

          Map<StoreType, Path> newDirMap = new HashMap<>();
          for (StoreType storeType : StoreType.values()) {
            Path newDirPath = (customDirs != null) ? customDirs.get(storeType) : null;

            if (newDirPath != null && !storeType.allowCustomDir()) {
              logger()
                  .error(
                      "Path of store type {} is not customizable, ignoring custom dir in"
                          + " MetaConfig update",
                      storeType.identifier());
              continue;
            }

            if (newDirPath == null) {
              newDirPath = defaultScopedDirs.get(storeType).resolve(scopeName);
            }

            newDirMap.put(storeType, newDirPath);
          }

          try {
            MapUtils.requireUniqueValues(newDirMap, "dir path for each StoreType");
          } catch (IllegalArgumentException e) {
            logger()
                .warn(
                    "MetaConfig update contains duplicate dir paths for scope `{}`, ignoring: {}",
                    scopeName,
                    e.getMessage());
            return;
          }

          pendingChanges.put(scope, newDirMap);
        });

    // Phase 2: Apply all validated changes.
    pendingChanges.forEach(
        (scopeStorage, newDirMap) ->
            newDirMap.forEach(
                (storeType, newDirPath) -> {
                  try {
                    Storage storage = scopeStorage.storage(storeType);
                    if (storage.getDirPath().equals(newDirPath)) {
                      return;
                    }

                    logger()
                        .info(
                            "Updating dir path for scope `{}`, store type `{}` from `{}` to `{}`",
                            scopeStorage.name(),
                            storeType,
                            storage.getDirPath(),
                            newDirPath);

                    storage.setDirPath(newDirPath);
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

  private static void validateDirs(Map<StoreType, Path> scopedDirs)
      throws IllegalArgumentException {
    // Check for missing StoreTypes.
    List<StoreType> missingTypes = StoreType.Utils.missingTypes(scopedDirs.keySet());
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
  public Scope scope(String scopeName) {
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

    if (!(MIN_SCOPE_NAME_LENGTH <= scopeName.length()
        && scopeName.length() <= MAX_SCOPE_NAME_LENGTH)) {
      return "scope length must be between "
          + MIN_SCOPE_NAME_LENGTH
          + " and "
          + MAX_SCOPE_NAME_LENGTH
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

  private Scope createScopeStorage(String scopeName) {
    // Build a directory map for the given scope, preferring custom directories from MetaConfig.
    Map<StoreType, Path> dirsForScope = new HashMap<>(defaultScopedDirs);
    try {
      // Load meta configuration which may contain per‑scope custom directory mappings.
      MetaConfig meta = metaConfig.get();
      ScopeMetaConfig perScope = meta.scopes().get(scopeName);
      if (perScope != null) {
        // Override default directories with any custom paths defined for this scope.
        perScope
            .getCustomDirs()
            .forEach(
                (storeType, path) -> {
                  if (storeType.allowCustomDir()) {
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
    return new ScopeImpl(this, scopeName, new SystemLogger(logManager, scopeName), dirsForScope);
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

  public static final class BuilderImpl implements Builder {

    private Path logsDir;
    private Path metaConfigDir;
    private final Map<StoreType, Path> scopedDirs = new HashMap<>();
    private long maxLogFileSize = 10 * 1024 * 1024;
    private int maxLogArchiveFiles = 10;

    public BuilderImpl() {}

    @Override
    public SystemStorageLibImpl build() {
      if (logsDir == null || metaConfigDir == null) {
        BaseDirectories baseDirs = BaseDirectories.get();
        if (logsDir == null) {
          logsDir = Path.of(baseDirs.dataDir, ROOT_DIR_NAME, "logs");
        }
        if (metaConfigDir == null) {
          metaConfigDir = Path.of(baseDirs.configDir, ROOT_DIR_NAME, "metaconfig");
        }
      }
      return new SystemStorageLibImpl(
          logsDir, metaConfigDir, scopedDirs, maxLogFileSize, maxLogArchiveFiles);
    }

    @Override
    public BuilderImpl logsDir(Path logsDir) {
      this.logsDir = logsDir;
      return this;
    }

    @Override
    public BuilderImpl metaConfigDir(Path metaConfigDir) {
      this.metaConfigDir = metaConfigDir;
      return this;
    }

    /// Set the root directory for a store type.
    /// The scoped directory will be `rootDir / <scope>`.
    @Override
    public BuilderImpl storeDir(StoreType storeType, Path rootDir) {
      scopedDirs.put(storeType, rootDir);
      return this;
    }

    @Override
    public BuilderImpl maxLogFileSize(long maxLogFileSize) {
      this.maxLogFileSize = maxLogFileSize;
      return this;
    }

    @Override
    public BuilderImpl maxLogArchiveFiles(int maxLogArchiveFiles) {
      this.maxLogArchiveFiles = maxLogArchiveFiles;
      return this;
    }
  }
}
