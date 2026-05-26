package io.github.leawind.systemstoragelib.v1.utils;

import com.mojang.serialization.Codec;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import java.nio.file.Path;

public class Codecs {
  public static final Codec<Path> PATH = Codec.STRING.xmap(Path::of, Path::toString);

  public static final Codec<StoreType> STORE_TYPE =
      Codec.STRING.xmap(StoreType::of, StoreType::identifier);
}
