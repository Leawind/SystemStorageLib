package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import java.util.Objects;

public final class MetaConfigImpl implements MetaConfig {
  private static final long DEFAULT_MAX_LOG_FILE_SIZE = 1024 * 1024;
  private static final int DEFAULT_MAX_LOG_ARCHIVE_FILES = 16;

  public static Codec<MetaConfig> codec(SystemStorageLib lib) {
    return RecordCodecBuilder.create(
        inst ->
            inst.group(
                    Codec.LONG
                        .optionalFieldOf("max_log_file_size", DEFAULT_MAX_LOG_FILE_SIZE)
                        .forGetter(MetaConfig::getMaxLogFileSize),
                    Codec.INT
                        .optionalFieldOf("max_log_archive_files", DEFAULT_MAX_LOG_ARCHIVE_FILES)
                        .forGetter(MetaConfig::getMaxLogArchiveFiles))
                .apply(
                    inst,
                    (maxLogFileSize, maxLogArchiveFiles) ->
                        new MetaConfigImpl(lib, maxLogFileSize, maxLogArchiveFiles)));
  }

  private long maxLogFileSize;
  private int maxLogArchiveFiles;

  MetaConfigImpl(SystemStorageLib lib, long maxLogFileSize, int maxLogArchiveFiles) {
    this.maxLogFileSize = maxLogFileSize;
    this.maxLogArchiveFiles = maxLogArchiveFiles;
  }

  MetaConfigImpl(SystemStorageLib lib) {
    this(lib, DEFAULT_MAX_LOG_FILE_SIZE, DEFAULT_MAX_LOG_ARCHIVE_FILES);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof MetaConfig config) {

      return getMaxLogFileSize() == config.getMaxLogFileSize()
          && getMaxLogArchiveFiles() == config.getMaxLogArchiveFiles();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxLogFileSize, maxLogArchiveFiles);
  }
}
