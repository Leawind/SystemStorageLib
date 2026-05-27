package io.github.leawind.systemstoragelib.v1.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class CodecsTest {

  /** The innermost data class */
  public record Inner(int value, String label) {
    public static final Codec<Inner> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        Codec.INT.fieldOf("value").forGetter(Inner::value),
                        Codec.STRING.fieldOf("label").forGetter(Inner::label))
                    .apply(instance, Inner::new));
  }

  /** Middle layer, contains an Inner and an unboundedMap */
  public record Middle(
      String name, Inner inner, Map<String, String> properties // encoded using Codec.unboundedMap
      ) {
    public static final Codec<Middle> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        Codec.STRING.fieldOf("name").forGetter(Middle::name),
                        Inner.CODEC.fieldOf("inner").forGetter(Middle::inner),
                        // unboundedMap with String keys and String values
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .fieldOf("properties")
                            .forGetter(Middle::properties))
                    .apply(instance, Middle::new));
  }

  /** Outer layer, contains a Middle and a list of strings */
  public record Outer(String id, Middle middle, List<String> tags) {
    public static final Codec<Outer> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        Codec.STRING.fieldOf("id").forGetter(Outer::id),
                        Middle.CODEC.fieldOf("middle").forGetter(Outer::middle),
                        Codec.STRING.listOf().fieldOf("tags").forGetter(Outer::tags))
                    .apply(instance, Outer::new));
  }

  @Test
  public void testCloneInteger() {
    Integer original = 42;
    Integer cloned = Codecs.clone(original, Codec.INT, JsonOps.INSTANCE);
    assertEquals(original, cloned);
  }

  @Test
  public void testCloneString() {
    String original = "Hello, World!";
    String cloned = Codecs.clone(original, Codec.STRING, JsonOps.INSTANCE);
    assertEquals(original, cloned);
  }

  @Test
  public void testCloneWithNullThrows() {
    assertThrows(RuntimeException.class, () -> Codecs.clone(null, Codec.INT, JsonOps.INSTANCE));
  }

  @Test
  public void testCloneComplexObject() {
    // Build a deeply nested object
    Inner inner = new Inner(10, "inner-label");
    Middle middle =
        new Middle(
            "middle-name", inner, Map.of("key1", "val1", "key2", "val2")); // unboundedMap data
    Outer original = new Outer("outer-id", middle, List.of("tagA", "tagB"));

    Outer cloned = Codecs.clone(original, Outer.CODEC, JsonOps.INSTANCE);

    // 1. Outer: equal in value but different reference
    assertEquals(original, cloned);
    assertNotSame(original, cloned);

    // 2. Middle: different reference, equal in value
    assertNotSame(original.middle(), cloned.middle());
    assertEquals(original.middle(), cloned.middle());

    // 3. Inner: different reference, equal in value
    assertNotSame(original.middle().inner(), cloned.middle().inner());
    assertEquals(original.middle().inner(), cloned.middle().inner());

    // 4. unboundedMap: different reference, same entries
    Map<String, String> origProps = original.middle().properties();
    Map<String, String> clonedProps = cloned.middle().properties();
    assertNotSame(origProps, clonedProps);
    assertEquals(origProps, clonedProps);

    // 5. List: different reference, same elements
    List<String> origTags = original.tags();
    List<String> clonedTags = cloned.tags();
    assertNotSame(origTags, clonedTags);
    assertEquals(origTags, clonedTags);
  }
}
