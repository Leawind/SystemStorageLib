package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import java.util.Map;

/// Configuration for the entire library, including per-scope custom directory mappings.
public interface MetaConfig {
  // region scopes

  Map<String, ScopeMetaConfig> scopes();

  ScopeMetaConfig scope(String scopeName);

  // endregion

  // region logConfig
  int getMaxLogFileSize();

  void setMaxLogFileSize(int value);

  int getMaxLogArchiveFiles();

  void setMaxLogArchiveFiles(int value);
  // endregion
}
