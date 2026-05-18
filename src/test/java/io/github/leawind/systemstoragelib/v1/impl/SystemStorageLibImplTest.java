package io.github.leawind.systemstoragelib.v1.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.managers.MetaConfigManager;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SystemStorageLibImplTest {

  @TempDir Path tempDir;

  private Map<StoreType<?>, Path> allDirs() {
    Map<StoreType<?>, Path> dirs = new HashMap<>();
    dirs.put(StoreType.CREDENTIALS, tempDir.resolve("credentials"));
    dirs.put(StoreType.CONFIG, tempDir.resolve("config"));
    dirs.put(StoreType.DATA, tempDir.resolve("data"));
    dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
    dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));
    return dirs;
  }

  private SystemStorageLibImpl createImpl() {
    return new SystemStorageLibImpl(
        tempDir.resolve("logs"), tempDir.resolve("metaconfig"), allDirs());
  }

  @Nested
  class Construction {
    @Test
    void createsWithValidDirs() {
      assertDoesNotThrow(SystemStorageLibImplTest.this::createImpl);
    }

    @Test
    void throwsWhenMissingStoreType() {
      Map<StoreType<?>, Path> dirs = allDirs();
      dirs.remove(StoreType.CREDENTIALS);
      var ex =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new SystemStorageLibImpl(
                      tempDir.resolve("logs"), tempDir.resolve("metaconfig"), dirs));
      assertTrue(ex.getMessage().contains("Missing StoreTypes"));
    }

    @Test
    void throwsWhenDuplicateDirPaths() {
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      Path shared = tempDir.resolve("shared");
      dirs.put(StoreType.CREDENTIALS, shared);
      dirs.put(StoreType.CONFIG, shared);
      dirs.put(StoreType.DATA, tempDir.resolve("data"));
      dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
      dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));
      var ex =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new SystemStorageLibImpl(
                      tempDir.resolve("logs"), tempDir.resolve("metaconfig"), dirs));
      assertTrue(ex.getMessage().contains("dir for each StoreType must be unique"));
    }

    @Test
    void throwsWhenAllMissing() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new SystemStorageLibImpl(
                  tempDir.resolve("logs"), tempDir.resolve("metaconfig"), Map.of()));
    }
  }

  @Nested
  class ScopeValidation {

    @Test
    void validScopeReturnsNull() {
      var impl = createImpl();
      assertNull(impl.validateScope("valid-scope"));
    }

    @Test
    void emptyScopeIsInvalid() {
      var impl = createImpl();
      assertNotNull(impl.validateScope(""));
    }

    @Test
    void tooShortScopeIsInvalid() {
      var impl = createImpl();
      assertNotNull(impl.validateScope("a"));
    }

    @Test
    void tooLongScopeIsInvalid() {
      var impl = createImpl();
      assertNotNull(impl.validateScope("a".repeat(1024)));
    }

    @Test
    void scopeStartingWithDashIsInvalid() {
      var impl = createImpl();
      String result = impl.validateScope("-invalid");
      assertNotNull(result);
      assertTrue(result.contains("must not start with"));
    }

    @Test
    void scopeStartingWithPlusIsInvalid() {
      var impl = createImpl();
      String result = impl.validateScope("+invalid");
      assertNotNull(result);
      assertTrue(result.contains("must not start with"));
    }

    @Test
    void scopeStartingWithDotIsInvalid() {
      var impl = createImpl();
      String result = impl.validateScope(".invalid");
      assertNotNull(result);
      assertTrue(result.contains("must not start with"));
    }

    @Test
    void scopeEndingWithDashIsInvalid() {
      var impl = createImpl();
      String result = impl.validateScope("invalid-");
      assertNotNull(result);
      assertTrue(result.contains("must not end with"));
    }

    @Test
    void scopeEndingWithPlusIsInvalid() {
      var impl = createImpl();
      String result = impl.validateScope("invalid+");
      assertNotNull(result);
      assertTrue(result.contains("must not end with"));
    }

    @Test
    void scopeEndingWithDotIsInvalid() {
      var impl = createImpl();
      String result = impl.validateScope("invalid.");
      assertNotNull(result);
      assertTrue(result.contains("must not end with"));
    }

    @Test
    void scopeWithInvalidCharacterIsInvalid() {
      var impl = createImpl();
      assertNotNull(impl.validateScope("invalid scope"));
    }

    @Test
    void scopeWithAllowedSpecialCharsIsValid() {
      var impl = createImpl();
      assertNull(impl.validateScope("my_scope-v1.2+feature"));
    }

    @Test
    void isScopeValidDelegatesToValidateScope() {
      var impl = createImpl();
      assertTrue(impl.isScopeValid("valid"));
      assertFalse(impl.isScopeValid(""));
    }

    @Test
    void scopeAtMinLengthIsValid() {
      var impl = createImpl();
      assertNull(impl.validateScope("ab"));
    }

    @Test
    void scopeAtMaxLengthIsValid() {
      var impl = createImpl();
      assertNull(impl.validateScope("a".repeat(63)));
    }
  }

  @Nested
  class ScopeCreation {

    @Test
    void scopeReturnsScopeStorage() {
      var impl = createImpl();
      var storage = impl.scope("test_scope");
      assertNotNull(storage);
      assertEquals("test_scope", storage.scope());
    }

    @Test
    void sameScopeReturnsSameInstance() {
      var impl = createImpl();
      var storage1 = impl.scope("test_scope");
      var storage2 = impl.scope("test_scope");
      assertEquals(storage1, storage2);
    }

    @Test
    void differentScopesReturnDifferentInstances() {
      var impl = createImpl();
      var storage1 = impl.scope("scope_a");
      var storage2 = impl.scope("scope_b");
      assertNotEquals(storage1, storage2);
    }
  }

  @Nested
  class LogsDir {

    @Test
    void getLogsDirReturnsProvidedPath() {
      Path logsDir = tempDir.resolve("logs");
      var impl = new SystemStorageLibImpl(logsDir, tempDir.resolve("metaconfig"), allDirs());
      assertEquals(logsDir, impl.getLogsDir());
    }
  }

  @Nested
  class MetaConfig {

    @Test
    void metaConfigReturnsNonNullManager() {
      var impl = createImpl();
      MetaConfigManager mgr = impl.metaConfig();
      assertNotNull(mgr);
    }
  }
}
