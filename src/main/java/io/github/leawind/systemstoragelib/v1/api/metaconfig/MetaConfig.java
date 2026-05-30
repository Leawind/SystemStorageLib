package io.github.leawind.systemstoragelib.v1.api.metaconfig;


/// Configuration for the entire library, including per-scope custom directory mappings.
public interface MetaConfig {

  // region logConfig
  long getMaxLogFileSize();

  void setMaxLogFileSize(long value);

  int getMaxLogArchiveFiles();

  void setMaxLogArchiveFiles(int value);
  // endregion
}
