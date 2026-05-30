package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import io.github.leawind.systemstoragelib.v1.utils.ScopeHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class MetaConfigImpl implements MetaConfig {
  private static final long DEFAULT_MAX_LOG_FILE_SIZE = 1024 * 1024;
  private static final int DEFAULT_MAX_LOG_ARCHIVE_FILES = 16;

  public static Codec<MetaConfig> codec(SystemStorageLib lib) {
    return RecordCodecBuilder.create(
        inst ->
            inst.group(
                    Codec.unboundedMap(Codec.STRING, ScopeMetaConfigImpl.CODEC)
                        .fieldOf("scopes")
                        .forGetter(MetaConfig::scopes),
                    Codec.LONG
                        .optionalFieldOf("max_log_file_size", DEFAULT_MAX_LOG_FILE_SIZE)
                        .forGetter(MetaConfig::getMaxLogFileSize),
                    Codec.INT
                        .optionalFieldOf("max_log_archive_files", DEFAULT_MAX_LOG_ARCHIVE_FILES)
                        .forGetter(MetaConfig::getMaxLogArchiveFiles))
                .apply(
                    inst,
                    (map, maxLogFileSize, maxLogArchiveFiles) -> new MetaConfigImpl(lib, map)));
  }

  private final Map<String, ScopeMetaConfig> scopes;

  private long maxLogFileSize;
  private int maxLogArchiveFiles;

  MetaConfigImpl(
      SystemStorageLib lib,
      @Nullable Map<String, ScopeMetaConfig> scopes,
      long maxLogFileSize,
      int maxLogArchiveFiles) {
    this.scopes = new ScopeHashMap<>(lib);

    if (scopes != null) {
      this.scopes.putAll(scopes);
    }
    this.maxLogFileSize = maxLogFileSize;
    this.maxLogArchiveFiles = maxLogArchiveFiles;
  }

  MetaConfigImpl(SystemStorageLib lib, @Nullable Map<String, ScopeMetaConfig> scopes) {
    this(lib, scopes, DEFAULT_MAX_LOG_FILE_SIZE, DEFAULT_MAX_LOG_ARCHIVE_FILES);
  }

  public MetaConfigImpl(SystemStorageLib lib) {
    this(lib, null);
  }

  // region scopes

  @Override
  public Map<String, ScopeMetaConfig> scopes() {
    return scopes;
  }

  @Override
  public ScopeMetaConfig scope(String scopeName) {
    return scopes.computeIfAbsent(scopeName, (ignored) -> new ScopeMetaConfigImpl());
  }

  @Override
  public long getMaxLogFileSize() {
    return maxLogFileSize;
  }

  @Override
  public void setMaxLogFileSize(long value) {
    if (value <= 1) {
      throw new IllegalArgumentException("maxLogFileSize is too small: " + value);
    }
    maxLogFileSize = value;
  }

  @Override
  public int getMaxLogArchiveFiles() {
    return maxLogArchiveFiles;
  }

  @Override
  public void setMaxLogArchiveFiles(int value) {
    if (value <= 1) {
      throw new IllegalArgumentException("maxLogArchiveFiles is too small: " + value);
    }
    maxLogArchiveFiles = value;
  }

  // endregion

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof MetaConfig config) {

      return scopes.equals(config.scopes())
          && getMaxLogFileSize() == config.getMaxLogFileSize()
          && getMaxLogArchiveFiles() == config.getMaxLogArchiveFiles();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(scopes(), maxLogFileSize, maxLogArchiveFiles);
  }
}
