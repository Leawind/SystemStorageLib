package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class MetaConfig {

  public static final Codec<MetaConfig> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(Codec.STRING, PerScopeConfig.CODEC)
                          .fieldOf("scopes")
                          .forGetter(MetaConfig::getScopesConfig))
                  .apply(inst, MetaConfig::new));

  private final Map<String, PerScopeConfig> scopesConfig;

  // region Codec Methods

  private MetaConfig(Map<String, PerScopeConfig> scopes) {
    this.scopesConfig = scopes;
  }

  private Map<String, PerScopeConfig> getScopesConfig() {
    return scopesConfig;
  }

  // endregion

  public @Nullable PerScopeConfig getScopeConfig(String scopeName) {
    return scopesConfig.get(scopeName);
  }

  public PerScopeConfig getOrCreateScopeConfig(String scopeName) {
    return scopesConfig.computeIfAbsent(scopeName, ignored -> PerScopeConfig.getDefault());
  }

  public void removeScopeConfig(String scopeName) {
    scopesConfig.remove(scopeName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MetaConfig other)) {
      return false;
    }
    return scopesConfig.equals(other.scopesConfig);
  }

  @Override
  public int hashCode() {
    return scopesConfig.hashCode();
  }

  public static MetaConfig getDefault() {
    return new MetaConfig(new HashMap<>());
  }
}
