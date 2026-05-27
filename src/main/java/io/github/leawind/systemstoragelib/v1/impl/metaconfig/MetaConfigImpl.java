package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import io.github.leawind.systemstoragelib.v1.utils.ScopeHashMap;
import java.util.Map;

public record MetaConfigImpl(SystemStorageLib lib, Map<String, ScopeMetaConfig> scopes)
    implements MetaConfig {

  public MetaConfigImpl(SystemStorageLib lib, Map<String, ScopeMetaConfig> scopes) {
    this.lib = lib;
    this.scopes = new ScopeHashMap<>(lib);
    this.scopes.putAll(scopes);
  }

  // region scopes

  @Override
  public ScopeMetaConfig scope(String scopeName) {
    return scopes.computeIfAbsent(scopeName, (ignored) -> new ScopeMetaConfigImpl());
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
