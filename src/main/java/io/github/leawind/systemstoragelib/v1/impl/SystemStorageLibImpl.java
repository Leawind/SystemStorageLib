package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfigAccessor;
import io.github.leawind.systemstoragelib.v1.impl.log.LogAccessor;
import io.github.leawind.systemstoragelib.v1.impl.log.SystemLogger;
import io.github.leawind.systemstoragelib.v1.impl.metaconfig.MetaConfigAccessorImpl;
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
import org.slf4j.LoggerFactory;

public class SystemStorageLibImpl implements SystemStorageLib {
  public static final Logger FALLBACK_LOGGER = LoggerFactory.getLogger(SystemStorageLibImpl.class);

  public static final String APP_NAME = "mc_system_storage";

  public static final int MIN_SCOPE_NAME_LENGTH = 2;
  public static final int MAX_SCOPE_NAME_LENGTH = 128;

  /// ### Examples
  ///
  /// - config: `/home/Steve/.config/<root_name>/config`.
  /// - data: `/home/Steve/.local/share/<root_name>/data`.
  private final Map<StoreType, Path> defaultScopedDirs;

  private final LogAccessor logAccessor;
  private final Logger logger;
  private final MetaConfigAccessor metaConfigAccessor;
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
      @NonNull Path metaConfigDir,
      @NonNull Path logsDir,
      @NonNull Map<StoreType, Path> defaultScopedDirs) {
    validateConstructorArgs(metaConfigDir, logsDir, defaultScopedDirs);

    this.defaultScopedDirs = new HashMap<>(defaultScopedDirs);

    logAccessor = new LogAccessor(logsDir, FALLBACK_LOGGER);
    logger = new SystemLogger(logAccessor, "-");
    
    metaConfigAccessor = new MetaConfigAccessorImpl(this, metaConfigDir, logger);
    metaConfigAccessor.setLogger(logger);

    // Listen for external changes to meta config and update scope storage paths accordingly.
    metaConfigAccessor.onChanged().on(this::handleMetaConfigChanged);

    scopes = new ConcurrentScopeHashMap<>(this);
    detectScopes();

    // Load meta config
    try {
      this.handleMetaConfigChanged(
          new MetaConfigAccessor.ChangedEvent(null, metaConfigAccessor.get()));
    } catch (IOException e) {
      logger.error("Failed to load meta config during initializing", e);
    }
  }

  private void validateConstructorArgs(
      @NonNull Path metaConfigDir,
      @NonNull Path logsDir,
      @NonNull Map<StoreType, Path> defaultScopedDirs) {
    Objects.requireNonNull(logsDir);
    Objects.requireNonNull(metaConfigDir);

    validateDirs(defaultScopedDirs);

    metaConfigDir = metaConfigDir.toAbsolutePath().normalize();
    if (defaultScopedDirs.containsValue(metaConfigDir)) {
      throw new IllegalArgumentException("metaConfigDir is already used: " + metaConfigDir);
    }

    logsDir = logsDir.toAbsolutePath().normalize();
    if (defaultScopedDirs.containsValue(logsDir)) {
      throw new IllegalArgumentException("logsDir is already used: " + logsDir);
    }
  }

  private void handleMetaConfigChanged(MetaConfigAccessor.ChangedEvent event) {
    var config = event.config();

    // log configs
    logAccessor.setMaxFileSize(config.getMaxLogFileSize());
    logAccessor.setMaxArchiveFiles(config.getMaxLogArchiveFiles());
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
  public MetaConfigAccessor metaConfig() {
    return metaConfigAccessor;
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
  public Map<String, Optional<Scope>> scopes() {
    detectScopes();
    return scopes;
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
    return logAccessor.getDirPath();
  }

  private Scope createScopeStorage(String scopeName) {
    // Build a directory map for the given scope, preferring custom directories from MetaConfig.
    return new ScopeImpl(
        scopeName, new SystemLogger(logAccessor, scopeName), new HashMap<>(defaultScopedDirs));
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

    private final Path metaConfigDir;
    private Path logsDir;
    private final Map<StoreType, Path> scopedDirs = new HashMap<>();

    public BuilderImpl(Path metaConfigDir) {
      this.metaConfigDir = Objects.requireNonNull(metaConfigDir);
    }

    @Override
    public SystemStorageLibImpl build() {
      return new SystemStorageLibImpl(metaConfigDir, logsDir, scopedDirs);
    }

    @Override
    public BuilderImpl logsDir(Path logsDir) {
      this.logsDir = Objects.requireNonNull(logsDir);
      return this;
    }

    /// Set the root directory for a store type.
    /// The scoped directory will be `rootDir / <scope>`.
    @Override
    public BuilderImpl storeDir(StoreType storeType, Path rootDir) {
      scopedDirs.put(storeType, rootDir);
      return this;
    }
  }
}
