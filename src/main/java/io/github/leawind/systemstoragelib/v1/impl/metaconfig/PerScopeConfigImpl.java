package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.PerScopeConfig;
import io.github.leawind.systemstoragelib.v1.utils.Codecs;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class PerScopeConfigImpl implements PerScopeConfig {
  public static final Codec<PerScopeConfig> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(StoreType.CODEC, Codecs.PATH)
                          .fieldOf("custom_dirs")
                          .forGetter(PerScopeConfig::getCustomDirs))
                  .apply(inst, PerScopeConfigImpl::new));

  private final CustomDirMap customDirs = new CustomDirMap();

  public PerScopeConfigImpl(Map<StoreType<?>, Path> customDirs) {
    this.customDirs.putAll(customDirs);
  }

  // region customDirs

  @Override
  public Map<StoreType<?>, Path> getCustomDirs() {
    return customDirs;
  }

  @Override
  public void setCustomDirs(Map<StoreType<?>, Path> m) {
    customDirs.validateMap(m);
    customDirs.clear();
    customDirs.putAll(m);
  }

  // endregion

  // region Object

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof PerScopeConfig other) {
      return Objects.equals(customDirs, other.getCustomDirs());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(customDirs);
  }

  @Override
  public String toString() {
    return "PerScopeConfig[" + "customDirs=" + customDirs + ']';
  }

  // endregion
}
