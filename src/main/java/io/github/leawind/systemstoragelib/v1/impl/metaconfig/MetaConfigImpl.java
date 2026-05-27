package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import io.github.leawind.systemstoragelib.v1.utils.ScopeHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class MetaConfigImpl implements MetaConfig {
  public static Codec<MetaConfig> codec(SystemStorageLib lib) {
    return RecordCodecBuilder.create(
        inst ->
            inst.group(
                    Codec.unboundedMap(Codec.STRING, ScopeMetaConfigImpl.CODEC)
                        .fieldOf("scopes")
                        .forGetter(MetaConfig::scopes))
                .apply(inst, (map) -> new MetaConfigImpl(lib, map)));
  }

  private final Map<String, ScopeMetaConfig> scopes;

  MetaConfigImpl(SystemStorageLib lib, @Nullable Map<String, ScopeMetaConfig> scopes) {
    this.scopes = new ScopeHashMap<>(lib);
    if (scopes != null) {
      this.scopes.putAll(scopes);
    }
  }

  MetaConfigImpl(SystemStorageLib lib) {
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
