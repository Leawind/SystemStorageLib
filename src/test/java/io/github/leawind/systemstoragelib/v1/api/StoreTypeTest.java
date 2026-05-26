package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.utils.Codecs;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StoreTypeTest extends BaseTest {

  @Nested
  class Values {

    @Test
    void valuesReturnsAllFiveTypes() {
      StoreType[] values = StoreType.values();
      assertEquals(5, values.length);
    }

    @Test
    void valuesContainsAllConstants() {
      Set<StoreType> values = new HashSet<>();
      java.util.Collections.addAll(values, StoreType.values());
      assertTrue(values.contains(StoreType.CREDENTIALS));
      assertTrue(values.contains(StoreType.CONFIG));
      assertTrue(values.contains(StoreType.DATA));
      assertTrue(values.contains(StoreType.CACHE));
      assertTrue(values.contains(StoreType.DATA_LOCAL));
    }
  }

  @Nested
  class UtilsTest {

    @Test
    void missingTypesReturnsEmptyWhenAllPresent() {
      List<StoreType> missing =
          StoreType.Utils.missingTypes(
              Set.of(
                  StoreType.CREDENTIALS,
                  StoreType.CONFIG,
                  StoreType.DATA,
                  StoreType.CACHE,
                  StoreType.DATA_LOCAL));
      assertTrue(missing.isEmpty());
    }

    @Test
    void missingTypesDetectsSingleMissing() {
      List<StoreType> missing =
          StoreType.Utils.missingTypes(
              Set.of(StoreType.CONFIG, StoreType.DATA, StoreType.CACHE, StoreType.DATA_LOCAL));
      assertEquals(1, missing.size());
      assertEquals(StoreType.CREDENTIALS, missing.get(0));
    }

    @Test
    void missingTypesDetectsMultipleMissing() {
      List<StoreType> missing =
          StoreType.Utils.missingTypes(Set.of(StoreType.CONFIG, StoreType.DATA));
      assertEquals(3, missing.size());
      assertTrue(missing.contains(StoreType.CREDENTIALS));
      assertTrue(missing.contains(StoreType.CACHE));
      assertTrue(missing.contains(StoreType.DATA_LOCAL));
    }

    @Test
    void missingTypesReturnsAllWhenNonePresent() {
      List<StoreType> missing = StoreType.Utils.missingTypes(Set.of());
      assertEquals(5, missing.size());
    }
  }

  @Nested
  class Of {

    @Test
    void fromIdentifierCredentials() {
      assertEquals(StoreType.CREDENTIALS, StoreType.fromId("credentials"));
    }

    @Test
    void fromIdentifierConfig() {
      assertEquals(StoreType.CONFIG, StoreType.fromId("config"));
    }

    @Test
    void fromIdentifierData() {
      assertEquals(StoreType.DATA, StoreType.fromId("data"));
    }

    @Test
    void fromIdentifierCache() {
      assertEquals(StoreType.CACHE, StoreType.fromId("cache"));
    }

    @Test
    void fromIdentifierDataLocal() {
      assertEquals(StoreType.DATA_LOCAL, StoreType.fromId("data_local"));
    }

    @Test
    void fromIdentifierUnknownThrows() {
      assertThrows(IllegalArgumentException.class, () -> StoreType.fromId("unknown"));
    }

    @Test
    void fromIdentifierEmptyThrows() {
      assertThrows(IllegalArgumentException.class, () -> StoreType.fromId(""));
    }
  }

  @Nested
  class CodecRoundTrip {

    @Test
    void codecEncodesIdentifier() {
      DataResult<com.google.gson.JsonElement> result =
          Codecs.STORE_TYPE.encodeStart(JsonOps.INSTANCE, StoreType.CONFIG);
      assertTrue(result.result().isPresent());
      assertEquals("config", result.result().get().getAsString());
    }

    @Test
    void codecDecodesIdentifier() {
      DataResult<StoreType> result =
          Codecs.STORE_TYPE.parse(JsonOps.INSTANCE, new com.google.gson.JsonPrimitive("data"));
      assertTrue(result.result().isPresent());
      assertEquals(StoreType.DATA, result.result().get());
    }

    @Test
    void codecRoundTripAllTypes() {
      for (StoreType type : StoreType.values()) {
        DataResult<JsonElement> encoded = Codecs.STORE_TYPE.encodeStart(JsonOps.INSTANCE, type);
        assertTrue(encoded.result().isPresent());
        DataResult<StoreType> decoded =
            Codecs.STORE_TYPE.parse(JsonOps.INSTANCE, encoded.result().get());
        assertTrue(decoded.result().isPresent());
        assertEquals(type, decoded.result().get());
      }
    }
  }

  @Nested
  class IdentifierUniqueness {

    @Test
    void allIdentifiersAreUnique() {
      StoreType[] values = StoreType.values();
      long distinctCount = java.util.Arrays.stream(values).map(StoreType::id).distinct().count();
      assertEquals(values.length, distinctCount, "All StoreType identifiers must be unique");
    }
  }
}
