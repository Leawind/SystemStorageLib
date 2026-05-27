package io.github.leawind.systemstoragelib.v1.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import java.nio.file.Path;

public class Codecs {
  public static final Codec<Path> PATH = Codec.STRING.xmap(Path::of, Path::toString);

  public static final Codec<StoreType> STORE_TYPE =
      Codec.STRING.xmap(StoreType::fromId, StoreType::id);

  public static <A, T> A clone(A value, Codec<A> codec, DynamicOps<T> ops) {
    return codec
        .parse(ops, codec.encodeStart(ops, value).getOrThrow(false, e -> {}))
        .getOrThrow(false, e -> {});
  }
}
