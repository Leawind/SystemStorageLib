package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import io.github.leawind.systemstoragelib.v1.impl.metaconfig.MetaConfigImpl;
import java.util.HashMap;
import java.util.Map;

public interface MetaConfig {
  static MetaConfig getDefault() {
    return new MetaConfigImpl(new HashMap<>());
  }

  // region scopes

  Map<String, PerScopeConfig> scopes();

  PerScopeConfig getOrCreateScopeConfig(String scopeName);

  // endregion
}
