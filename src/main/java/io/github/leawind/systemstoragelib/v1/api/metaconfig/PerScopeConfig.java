package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.utils.Codecs;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PerScopeConfig {
  public static final Codec<PerScopeConfig> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(StoreType.CODEC, Codecs.PATH)
                          .fieldOf("custom_dirs")
                          .forGetter(PerScopeConfig::customDirs))
                  .apply(inst, PerScopeConfig::new));

  private final Map<StoreType<?>, Path> customDirs;

  public PerScopeConfig(Map<StoreType<?>, Path> customDirs) {
    this.customDirs = customDirs;
  }

  public Map<StoreType<?>, Path> customDirs() {
    return customDirs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PerScopeConfig)) {
      return false;
    }
    return Objects.equals(customDirs, ((PerScopeConfig) o).customDirs);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(customDirs);
  }

  public static PerScopeConfig getDefault() {
    return new PerScopeConfig(new HashMap<>());
  }
}
