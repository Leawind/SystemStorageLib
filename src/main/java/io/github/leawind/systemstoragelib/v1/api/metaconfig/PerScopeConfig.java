package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.utils.Codecs;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record PerScopeConfig(Map<StoreType<?>, Path> customDirs) {
  public static final Codec<PerScopeConfig> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(StoreType.CODEC, Codecs.PATH)
                          .fieldOf("custom_dirs")
                          .forGetter(PerScopeConfig::customDirs))
                  .apply(inst, PerScopeConfig::new));

  public static PerScopeConfig getDefault() {
    return new PerScopeConfig(new HashMap<>());
  }
}
