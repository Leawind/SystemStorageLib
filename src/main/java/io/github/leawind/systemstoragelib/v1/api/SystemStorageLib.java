package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.exception.SystemStorageLibException;
import io.github.leawind.systemstoragelib.v1.api.managers.MetaConfigManager;
import io.github.leawind.systemstoragelib.v1.impl.SystemStorageLibImpl;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface SystemStorageLib {

  /// Logger for this library instance, writing to both SLF4J and the library's own log file.
  ///
  /// @return the logger associated with this library instance
  Logger logger();

  /// Manager for meta configuration.
  ///
  /// @return the {@link MetaConfigManager} for reading and writing configuration
  MetaConfigManager metaConfig();

  /// Creates or retrieves a {@link ScopeStorage} for the given scope name.
  ///
  /// @param scope the scope name, must pass {@link #validateScope}
  /// @return the scope storage instance
  /// @throws IllegalArgumentException if the scope name is invalid
  ScopeStorage scope(String scope);

  /// Returns all known scope names, both from the in-memory cache and
  /// detected on disk.
  ///
  /// @return a stream of scope names
  Stream<String> getAllScopes();

  /// Validates a scope name.
  ///
  /// - `2 <= length <= 128`
  /// - Must not start or end with `-`, `+`, `.`
  /// - Allowed characters:
  ///   - digits
  ///   - ASCII letters, lowercase and uppercase
  ///   - chars like `_`, `-`, `+`, `.`
  ///
  /// @param scope the scope name to validate
  /// @return {@code null} if valid, otherwise a description of the validation failure
  @Nullable String validateScope(String scope);

  default boolean isScopeValid(String scope) {
    return validateScope(scope) == null;
  }

  /// Returns the path to the logs directory.
  ///
  /// @return the absolute path to the logs directory
  Path getLogsDir();

  /// Returns the singleton {@link SystemStorageLib} instance
  /// using platform-default directories.
  ///
  /// @return the singleton instance
  /// @throws SystemStorageLibException if initialization failed
  static SystemStorageLib getInstance() {
    return SystemStorageLibHolder.getInstance();
  }

  /// Creates a {@link SystemStorageLib} instance with the given configuration.
  ///
  /// Usually for testing.
  ///
  /// @param config the configuration
  /// @return a new instance
  /// @throws SystemStorageLibException if initialization fails
  /// @throws IllegalArgumentException if the configuration is invalid
  static SystemStorageLib create(SystemStorageLibConfig config) {
    return new SystemStorageLibImpl(
        config.logsDir(),
        config.metaConfigDir(),
        config.scopedDirs(),
        config.maxLogFileSize(),
        config.maxLogArchiveFiles());
  }
}
