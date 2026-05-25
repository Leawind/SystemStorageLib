package io.github.leawind.systemstoragelib.v1.api;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/// Immutable configuration for creating a {@link SystemStorageLib} instance.
///
/// Use {@link #builder()} to construct.
public final class SystemStorageLibConfig {
  private final Path logsDir;
  private final Path metaConfigDir;
  private final Map<StoreType<?>, Path> scopedDirs;
  private final long maxLogFileSize;
  private final int maxLogArchiveFiles;

  private SystemStorageLibConfig(Builder builder) {
    this.logsDir = builder.logsDir;
    this.metaConfigDir = builder.metaConfigDir;
    this.scopedDirs = Map.copyOf(builder.scopedDirs);
    this.maxLogFileSize = builder.maxLogFileSize;
    this.maxLogArchiveFiles = builder.maxLogArchiveFiles;
  }

  public Path logsDir() {
    return logsDir;
  }

  public Path metaConfigDir() {
    return metaConfigDir;
  }

  public Map<StoreType<?>, Path> scopedDirs() {
    return scopedDirs;
  }

  public long maxLogFileSize() {
    return maxLogFileSize;
  }

  public int maxLogArchiveFiles() {
    return maxLogArchiveFiles;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Path logsDir;
    private Path metaConfigDir;
    private final Map<StoreType<?>, Path> scopedDirs = new HashMap<>();
    private long maxLogFileSize = 10 * 1024 * 1024;
    private int maxLogArchiveFiles = 10;

    private Builder() {}

    public Builder logsDir(Path logsDir) {
      this.logsDir = logsDir;
      return this;
    }

    public Builder metaConfigDir(Path metaConfigDir) {
      this.metaConfigDir = metaConfigDir;
      return this;
    }

    /// Set the root directory for a store type.
    /// The scoped directory will be `rootDir / <scope>`.
    public Builder storeDir(StoreType<?> storeType, Path rootDir) {
      scopedDirs.put(storeType, rootDir);
      return this;
    }

    public Builder maxLogFileSize(long maxLogFileSize) {
      this.maxLogFileSize = maxLogFileSize;
      return this;
    }

    public Builder maxLogArchiveFiles(int maxLogArchiveFiles) {
      this.maxLogArchiveFiles = maxLogArchiveFiles;
      return this;
    }

    public SystemStorageLibConfig build() {
      if (logsDir == null) {
        throw new IllegalArgumentException("logsDir must not be null");
      }
      if (metaConfigDir == null) {
        throw new IllegalArgumentException("metaConfigDir must not be null");
      }
      if (scopedDirs.isEmpty()) {
        throw new IllegalArgumentException("scopedDirs must not be empty");
      }
      return new SystemStorageLibConfig(this);
    }
  }
}
