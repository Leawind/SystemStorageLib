package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import java.util.Map;

public interface MetaConfig {
  // region scopes

  Map<String, PerScopeConfig> scopes();

  PerScopeConfig getScopeConfig(String scopeName);

  // endregion
}
