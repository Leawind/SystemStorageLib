package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.PerScopeConfig;
import java.util.HashMap;
import java.util.Map;

public record MetaConfigImpl(SystemStorageLib lib, Map<String, PerScopeConfig> scopes)
    implements MetaConfig {

  public MetaConfigImpl(SystemStorageLib lib, Map<String, PerScopeConfig> scopes) {
    this.lib = lib;
    this.scopes = new HashMap<>(scopes);
  }

  // region scopes

  @Override
  public PerScopeConfig getScopeConfig(String scopeName) {
    lib.validateScopeName(scopeName);
    return scopes.computeIfAbsent(scopeName, ignored -> PerScopeConfig.createDefault());
  }

  // endregion

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MetaConfig)) {
      return false;
    }
    return scopes.equals(((MetaConfig) o).scopes());
  }

  @Override
  public int hashCode() {
    return scopes.hashCode();
  }
}
