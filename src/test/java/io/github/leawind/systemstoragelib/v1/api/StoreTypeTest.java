package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.api.managers.CredentialStore;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreTypeTest {

  private static final Logger TEST_LOGGER = LoggerFactory.getLogger(StoreTypeTest.class);

  @Nested
  class Constants {

    @Test
    void credentialsType() {
      assertEquals(CredentialStore.class, StoreType.CREDENTIALS.managerClass());
      assertEquals("credentials", StoreType.CREDENTIALS.identifier());
      assertFalse(StoreType.CREDENTIALS.customizable());
    }

    @Test
    void configType() {
      assertEquals(StorageManager.class, StoreType.CONFIG.managerClass());
      assertEquals("config", StoreType.CONFIG.identifier());
      assertTrue(StoreType.CONFIG.customizable());
    }

    @Test
    void dataType() {
      assertEquals(StorageManager.class, StoreType.DATA.managerClass());
      assertEquals("data", StoreType.DATA.identifier());
      assertTrue(StoreType.DATA.customizable());
    }

    @Test
    void cacheType() {
      assertEquals(StorageManager.class, StoreType.CACHE.managerClass());
      assertEquals("cache", StoreType.CACHE.identifier());
      assertTrue(StoreType.CACHE.customizable());
    }

    @Test
    void dataLocalType() {
      assertEquals(StorageManager.class, StoreType.DATA_LOCAL.managerClass());
      assertEquals("data_local", StoreType.DATA_LOCAL.identifier());
      assertTrue(StoreType.DATA_LOCAL.customizable());
    }
  }

  @Nested
  class Values {

    @Test
    void valuesReturnsAllFiveTypes() {
      var values = StoreType.values();
      assertEquals(5, values.length);
    }

    @Test
    void valuesContainsAllConstants() {
      var values = Set.of(StoreType.values());
      assertTrue(values.contains(StoreType.CREDENTIALS));
      assertTrue(values.contains(StoreType.CONFIG));
      assertTrue(values.contains(StoreType.DATA));
      assertTrue(values.contains(StoreType.CACHE));
      assertTrue(values.contains(StoreType.DATA_LOCAL));
    }
  }

  @Nested
  class Manager {

    @TempDir Path tempDir;

    @Test
    void credentialsManagerFactoryCreatesCredentialStore() {
      CredentialStore store = StoreType.CREDENTIALS.manager(TEST_LOGGER, tempDir.resolve("cred"));
      assertNotNull(store);
      assertEquals(tempDir.resolve("cred"), store.getDirPath());
    }

    @Test
    void configManagerFactoryCreatesStorageManager() {
      StorageManager mgr = StoreType.CONFIG.manager(TEST_LOGGER, tempDir.resolve("cfg"));
      assertNotNull(mgr);
      assertEquals(tempDir.resolve("cfg"), mgr.getDirPath());
    }

    @Test
    void dataManagerFactoryCreatesStorageManager() {
      StorageManager mgr = StoreType.DATA.manager(TEST_LOGGER, tempDir.resolve("dat"));
      assertNotNull(mgr);
      assertEquals(tempDir.resolve("dat"), mgr.getDirPath());
    }

    @Test
    void cacheManagerFactoryCreatesStorageManager() {
      StorageManager mgr = StoreType.CACHE.manager(TEST_LOGGER, tempDir.resolve("cch"));
      assertNotNull(mgr);
      assertEquals(tempDir.resolve("cch"), mgr.getDirPath());
    }

    @Test
    void dataLocalManagerFactoryCreatesStorageManager() {
      StorageManager mgr = StoreType.DATA_LOCAL.manager(TEST_LOGGER, tempDir.resolve("dl"));
      assertNotNull(mgr);
      assertEquals(tempDir.resolve("dl"), mgr.getDirPath());
    }
  }

  @Nested
  class UtilsTest {

    @Test
    void missingTypesReturnsEmptyWhenAllPresent() {
      List<StoreType<?>> missing =
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
      List<StoreType<?>> missing =
          StoreType.Utils.missingTypes(
              Set.of(StoreType.CONFIG, StoreType.DATA, StoreType.CACHE, StoreType.DATA_LOCAL));
      assertEquals(1, missing.size());
      assertEquals(StoreType.CREDENTIALS, missing.get(0));
    }

    @Test
    void missingTypesDetectsMultipleMissing() {
      List<StoreType<?>> missing =
          StoreType.Utils.missingTypes(Set.of(StoreType.CONFIG, StoreType.DATA));
      assertEquals(3, missing.size());
      assertTrue(missing.contains(StoreType.CREDENTIALS));
      assertTrue(missing.contains(StoreType.CACHE));
      assertTrue(missing.contains(StoreType.DATA_LOCAL));
    }

    @Test
    void missingTypesReturnsAllWhenNonePresent() {
      List<StoreType<?>> missing = StoreType.Utils.missingTypes(Set.of());
      assertEquals(5, missing.size());
    }
  }

  @Nested
  class IdentifierUniqueness {

    @Test
    void allIdentifiersAreUnique() {
      var values = StoreType.values();
      long distinctCount = Set.of(values).stream().map(StoreType::identifier).distinct().count();
      assertEquals(values.length, distinctCount, "All StoreType identifiers must be unique");
    }
  }
}
