package io.github.leawind.systemstoragelib.v1.utils;

import com.mojang.serialization.Codec;
import java.nio.file.Path;

public class Codecs {
  public static final Codec<Path> PATH = Codec.STRING.xmap(Path::of, Path::toString);
}
