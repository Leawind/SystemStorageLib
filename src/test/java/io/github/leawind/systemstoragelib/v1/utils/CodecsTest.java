package io.github.leawind.systemstoragelib.v1.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Test;

public class CodecsTest {

  /** A simple immutable object used to verify cloning of a custom Codec. */
  public record Person(String name, int age) {
    public static final Codec<Person> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        Codec.STRING.fieldOf("name").forGetter(Person::name),
                        Codec.INT.fieldOf("age").forGetter(Person::age))
                    .apply(instance, Person::new));
  }

  @Test
  public void testCloneInteger() {
    Integer original = 42;
    Integer cloned = Codecs.clone(original, Codec.INT, JsonOps.INSTANCE);

    assertEquals(original, cloned);
    // Integers in the cache pool (-128~127) return the same object; here we only verify value
    // equality.
    // Using a larger integer would allow verifying different references.
  }

  @Test
  public void testCloneString() {
    String original = "Hello, World!";
    String cloned = Codecs.clone(original, Codec.STRING, JsonOps.INSTANCE);

    assertEquals(original, cloned);
    // Strings are immutable; the cloned value must be equal.
  }

  @Test
  public void testCloneCustomObject() {
    Person original = new Person("Alice", 30);
    Person cloned = Codecs.clone(original, Person.CODEC, JsonOps.INSTANCE);

    assertEquals(original, cloned); // Values are equal
    assertNotSame(original, cloned); // But must be different object instances
  }

  @Test
  public void testCloneDeepEquality() {
    // Construct a slightly more complex Person to ensure deep copy semantics (value equality)
    Person original = new Person("Bob", 25);
    Person cloned = Codecs.clone(original, Person.CODEC, JsonOps.INSTANCE);

    assertEquals(original.name(), cloned.name());
    assertEquals(original.age(), cloned.age());
    assertEquals(original, cloned);
  }

  @Test
  public void testCloneWithNullThrows() {
    // Attempting to clone null: typically throws an exception based on Codec implementation
    assertThrows(RuntimeException.class, () -> Codecs.clone(null, Codec.INT, JsonOps.INSTANCE));
  }
}
