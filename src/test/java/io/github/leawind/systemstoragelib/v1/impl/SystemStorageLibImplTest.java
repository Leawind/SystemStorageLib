package io.github.leawind.systemstoragelib.v1.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfigStore;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SystemStorageLibImplTest extends BaseTest {
  private MetaConfigStore store;

  @BeforeEach
  void setupEach() {
    store = lib.metaConfig();
  }

  private Map<StoreType, Path> allDirs() {
    Map<StoreType, Path> dirs = new HashMap<>();
    dirs.put(StoreType.SECRETS, tempDir.resolve("secrets"));
    dirs.put(StoreType.CONFIG, tempDir.resolve("config"));
    dirs.put(StoreType.DATA, tempDir.resolve("data"));
    dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
    dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));
    return dirs;
  }

  private SystemStorageLibImpl createImpl() {
    return new SystemStorageLibImpl(
        tempDir.resolve("metaconfig"), tempDir.resolve("logs"), allDirs());
  }

  @Nested
  class Construction {
    @Test
    void createsWithValidDirs() {
      assertDoesNotThrow(SystemStorageLibImplTest.this::createImpl);
    }

    @Test
    void throwsWhenMissingStoreType() {
      Map<StoreType, Path> dirs = allDirs();
      dirs.remove(StoreType.SECRETS);
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new SystemStorageLibImpl(
                      tempDir.resolve("metaconfig"), tempDir.resolve("logs"), dirs));
      assertTrue(ex.getMessage().contains("Missing StoreTypes"));
    }

    @Test
    void throwsWhenDuplicateDirPaths() {
      Map<StoreType, Path> dirs = new HashMap<>();
      Path shared = tempDir.resolve("shared");
      dirs.put(StoreType.SECRETS, shared);
      dirs.put(StoreType.CONFIG, shared);
      dirs.put(StoreType.DATA, tempDir.resolve("data"));
      dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
      dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new SystemStorageLibImpl(
                      tempDir.resolve("metaconfig"), tempDir.resolve("logs"), dirs));
      assertTrue(ex.getMessage().contains("dir for each StoreType must be unique"));
    }

    @Test
    void throwsWhenAllMissing() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new SystemStorageLibImpl(
                  tempDir.resolve("metaconfig"), tempDir.resolve("logs"), Map.of()));
    }
  }

  @Nested
  class ScopeValidation {

    @Test
    void validScopeReturnsNull() {
      SystemStorageLibImpl impl = createImpl();
      assertNull(impl.validateScopeName("valid-scope"));
    }

    @Test
    void emptyScopeIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      assertNotNull(impl.validateScopeName(""));
    }

    @Test
    void tooShortScopeIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      assertNotNull(impl.validateScopeName("a"));
    }

    @Test
    void tooLongScopeIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      assertNotNull(impl.validateScopeName("a".repeat(1024)));
    }

    @Test
    void scopeStartingWithDashIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      String result = impl.validateScopeName("-invalid");
      assertNotNull(result);
      assertTrue(result.contains("must not start with"));
    }

    @Test
    void scopeStartingWithPlusIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      String result = impl.validateScopeName("+invalid");
      assertNotNull(result);
      assertTrue(result.contains("must not start with"));
    }

    @Test
    void scopeStartingWithDotIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      String result = impl.validateScopeName(".invalid");
      assertNotNull(result);
      assertTrue(result.contains("must not start with"));
    }

    @Test
    void scopeEndingWithDashIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      String result = impl.validateScopeName("invalid-");
      assertNotNull(result);
      assertTrue(result.contains("must not end with"));
    }

    @Test
    void scopeEndingWithPlusIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      String result = impl.validateScopeName("invalid+");
      assertNotNull(result);
      assertTrue(result.contains("must not end with"));
    }

    @Test
    void scopeEndingWithDotIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      String result = impl.validateScopeName("invalid.");
      assertNotNull(result);
      assertTrue(result.contains("must not end with"));
    }

    @Test
    void scopeWithInvalidCharacterIsInvalid() {
      SystemStorageLibImpl impl = createImpl();
      assertNotNull(impl.validateScopeName("invalid scope"));
    }

    @Test
    void scopeWithAllowedSpecialCharsIsValid() {
      SystemStorageLibImpl impl = createImpl();
      assertNull(impl.validateScopeName("my_scope-v1.2+feature"));
    }

    @Test
    void scopeAtMinLengthIsValid() {
      SystemStorageLibImpl impl = createImpl();
      assertNull(impl.validateScopeName("ab"));
    }

    @Test
    void scopeAtMaxLengthIsValid() {
      SystemStorageLibImpl impl = createImpl();
      char[] chars = new char[SystemStorageLibImpl.MAX_SCOPE_NAME_LENGTH];
      Arrays.fill(chars, 'a');
      assertNull(impl.validateScopeName(new String(chars)));
    }
  }

  @Nested
  class ScopeCreation {

    @Test
    void scopeReturnsScopeStorage() {
      SystemStorageLibImpl impl = createImpl();
      Scope storage = impl.scope("test_scope");
      assertNotNull(storage);
      assertEquals("test_scope", storage.name());
    }

    @Test
    void sameScopeReturnsSameInstance() {
      SystemStorageLibImpl impl = createImpl();
      Scope storage1 = impl.scope("test_scope");
      Scope storage2 = impl.scope("test_scope");
      assertEquals(storage1, storage2);
    }

    @Test
    void differentScopesReturnDifferentInstances() {
      SystemStorageLibImpl impl = createImpl();
      Scope storage1 = impl.scope("scope_a");
      Scope storage2 = impl.scope("scope_b");
      assertNotEquals(storage1, storage2);
    }
  }

  @Nested
  class LogsDir {

    @Test
    void getLogsDirReturnsProvidedPath() {
      Path logsDir = tempDir.resolve("logs");
      SystemStorageLibImpl impl =
          new SystemStorageLibImpl(tempDir.resolve("metaconfig"), logsDir, allDirs());
      assertEquals(logsDir, impl.getLogsDir());
    }
  }

  @Test
  void metaConfigReturnsNonNullManager() {
    SystemStorageLibImpl impl = createImpl();
    MetaConfigStore mgr = impl.metaConfig();
    assertNotNull(mgr);
  }
}
