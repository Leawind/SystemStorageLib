package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfigStore;
import io.github.leawind.systemstoragelib.v1.impl.SystemStorageLibImpl;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface SystemStorageLib {

  /// Logger for this library instance, writing to both SLF4J and the library's own log file.
  ///
  /// @return the logger associated with this library instance
  Logger logger();

  /// Manager for meta configuration.
  ///
  /// @return the {@link MetaConfigStore} for reading and writing configuration
  @ApiStatus.Experimental
  MetaConfigStore metaConfig();

  /// Creates or retrieves a {@link Scope} for the given scope name.
  ///
  /// @param scopeName the scope name, must pass {@link #validateScopeName}
  /// @return the name storage instance
  /// @throws IllegalArgumentException if the scope name is invalid
  Scope scope(String scopeName);

  /// Returns all known scope names, both from the in-memory cache and
  /// detected on disk.
  ///
  /// @return a map of scope names to scope instances
  @ApiStatus.Experimental
  Map<String, Optional<Scope>> scopes();

  /// Validates a scope name.
  ///
  /// - `2 <= length <= 128`
  /// - Must not start or end with `-`, `+`, `.`
  /// - Allowed characters:
  ///   - digits
  ///   - ASCII letters, lowercase and uppercase
  ///   - chars like `_`, `-`, `+`, `.`
  ///
  /// @param scopeName the scope name to validate
  /// @return {@code null} if valid, otherwise a description of the validation failure
  @ApiStatus.Experimental
  @Nullable String validateScopeName(String scopeName);

  /// Returns the path to the logs directory.
  ///
  /// @return the absolute path to the logs directory
  Path getLogsDir();

  /// Returns the singleton {@link SystemStorageLib} instance
  /// using platform-default directories.
  ///
  /// @return the singleton instance
  static SystemStorageLib getInstance() {
    return SystemStorageLibHolder.getInstance();
  }

  /// Returns a builder for creating a `SystemStorageLib` instance with custom configuration.
  ///
  /// Usually for testing.
  @ApiStatus.Experimental
  static Builder builder() {
    return new SystemStorageLibImpl.BuilderImpl();
  }

  /// Builder for creating a `SystemStorageLib` instance with custom configuration.
  interface Builder {
    /// Builds and returns the configured `SystemStorageLib` instance.
    SystemStorageLib build();

    /// Sets the directory for log files.
    Builder logsDir(Path logsDir);

    /// Sets the directory for meta configuration files.
    Builder metaConfigDir(Path metaConfigDir);

    /// The scoped directory will be `rootDir / <scope>`.
    Builder storeDir(StoreType storeType, Path rootDir);

    /// Sets the maximum size of a log file in bytes before rotation.
    Builder maxLogFileSize(long maxLogFileSize);

    /// Sets the maximum number of rotated archive log files.
    Builder maxLogArchiveFiles(int maxLogArchiveFiles);
  }
}
