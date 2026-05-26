package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.PerScopeConfig;
import java.util.HashMap;
import java.util.Map;

public record MetaConfigImpl(Map<String, PerScopeConfig> scopes) implements MetaConfig {

  public static final Codec<MetaConfig> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(Codec.STRING, PerScopeConfigImpl.CODEC)
                          .fieldOf("scopes")
                          .forGetter(MetaConfig::scopes))
                  .apply(inst, MetaConfigImpl::new));

  public MetaConfigImpl(Map<String, PerScopeConfig> scopes) {
    this.scopes = new HashMap<>(scopes);
  }

  public MetaConfigImpl() {
    this(new HashMap<>());
  }

  // region scopes

  @Override
  public PerScopeConfig getScopeConfig(String scopeName) {

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
