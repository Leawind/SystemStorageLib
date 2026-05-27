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
import io.github.leawind.systemstoragelib.v1.api.Storage;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfigStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SystemStorageLibImplTest extends BaseTest {
  private MetaConfigStore store;

  @BeforeEach
  void setupEach() {
    store = lib.metaConfig();
  }

  private Map<StoreType, Path> allDirs() {
    Map<StoreType, Path> dirs = new HashMap<>();
    dirs.put(StoreType.CREDENTIALS, tempDir.resolve("credentials"));
    dirs.put(StoreType.CONFIG, tempDir.resolve("config"));
    dirs.put(StoreType.DATA, tempDir.resolve("data"));
    dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
    dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));
    return dirs;
  }

  private SystemStorageLibImpl createImpl() {
    return new SystemStorageLibImpl(
        tempDir.resolve("logs"), tempDir.resolve("metaconfig"), allDirs(), 10 * 1024 * 1024, 10);
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
      dirs.remove(StoreType.CREDENTIALS);
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new SystemStorageLibImpl(
                      tempDir.resolve("logs"),
                      tempDir.resolve("metaconfig"),
                      dirs,
                      10 * 1024 * 1024,
                      10));
      assertTrue(ex.getMessage().contains("Missing StoreTypes"));
    }

    @Test
    void throwsWhenDuplicateDirPaths() {
      Map<StoreType, Path> dirs = new HashMap<>();
      Path shared = tempDir.resolve("shared");
      dirs.put(StoreType.CREDENTIALS, shared);
      dirs.put(StoreType.CONFIG, shared);
      dirs.put(StoreType.DATA, tempDir.resolve("data"));
      dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
      dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new SystemStorageLibImpl(
                      tempDir.resolve("logs"),
                      tempDir.resolve("metaconfig"),
                      dirs,
                      10 * 1024 * 1024,
                      10));
      assertTrue(ex.getMessage().contains("dir for each StoreType must be unique"));
    }

    @Test
    void throwsWhenAllMissing() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new SystemStorageLibImpl(
                  tempDir.resolve("logs"),
                  tempDir.resolve("metaconfig"),
                  Map.of(),
                  10 * 1024 * 1024,
                  10));
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
          new SystemStorageLibImpl(
              logsDir, tempDir.resolve("metaconfig"), allDirs(), 10 * 1024 * 1024, 10);
      assertEquals(logsDir, impl.getLogsDir());
    }
  }

  @Nested
  class MetaConfigAccess {

    @Test
    void metaConfigReturnsNonNullManager() {
      SystemStorageLibImpl impl = createImpl();
      MetaConfigStore mgr = impl.metaConfig();
      assertNotNull(mgr);
    }
  }

  @Nested
  class CustomDirsFromMetaConfig {

    @TempDir private Path customDir;

    @Test
    void scopeUsesCustomDirsWhenConfigured() throws IOException {
      SystemStorageLibImpl impl = createImpl();

      Path customDataDir = customDir.resolve("custom-data");
      impl.metaConfig()
          .update(
              config ->
                  config.scope("example_mod").getCustomDirs().put(StoreType.DATA, customDataDir));

      assertEquals(
          customDataDir.resolve("example_mod"),
          impl.scope("example_mod").storage(StoreType.DATA).getDirPath(),
          "DATA storage should use the custom dir from MetaConfig");
    }

    @Test
    void scopeUsesDefaultDirsWhenNoCustomConfig() {
      SystemStorageLibImpl impl = createImpl();

      // Should fall back to default
      assertEquals(
          tempDir.resolve("data").resolve("example_mod"),
          impl.scope("example_mod").storage(StoreType.DATA).getDirPath(),
          "DATA storage should use the default dir when no custom config");
    }

    @Test
    void unconfiguredStoreTypesUseDefault() throws IOException {
      SystemStorageLibImpl impl = createImpl();

      // Only configure custom dir for DATA
      Path customDataDir = tempDir.resolve("custom-data");
      impl.metaConfig()
          .update(
              config ->
                  config.scope("example_mod").getCustomDirs().put(StoreType.DATA, customDataDir));

      assertEquals(
          tempDir.resolve("config").resolve("example_mod"),
          impl.scope("example_mod").storage(StoreType.CONFIG).getDirPath(),
          "CONFIG storage should use default when only DATA has custom dir");
    }
  }

  @Nested
  class OnUpdateMetaConfig {

    @Test
    void setUpdatesExistingScopePaths() throws IOException {
      SystemStorageLibImpl impl = createImpl();

      // Create scope first with default dirs
      Scope storage = impl.scope("my-scope");
      Storage dataManager = storage.storage(StoreType.DATA);

      // Set a custom dir via set() — this should trigger onChanged and update existing scope
      Path customDataDir = tempDir.resolve("custom-data");
      impl.metaConfig()
          .update(
              config ->
                  config.scope("my-scope").getCustomDirs().put(StoreType.DATA, customDataDir));

      assertEquals(
          customDataDir,
          dataManager.getDirPath(),
          "DATA path should be updated after MetaConfig set()");
    }

    @Test
    void setToDefaultRevertsToDefaultPath() throws IOException {
      SystemStorageLibImpl impl = createImpl();

      // First set up a custom dir
      Path customDataDir = tempDir.resolve("custom-data");
      Scope storage = impl.scope("my-scope");
      Storage dataManager = storage.storage(StoreType.DATA);

      impl.metaConfig()
          .update(
              config ->
                  config.scope("my-scope").getCustomDirs().put(StoreType.DATA, customDataDir));

      assertEquals(
          customDataDir,
          dataManager.getDirPath(),
          "DATA path should use custom dir after first update");

      // Now set a config without the custom dir
      MetaConfig defaultConfig = store.get();
      impl.metaConfig()
          .update(
              oldConfig -> {
                oldConfig.scopes().clear();
                oldConfig.scopes().putAll(defaultConfig.scopes());
              });

      assertEquals(
          tempDir.resolve("data").resolve("my-scope"),
          dataManager.getDirPath(),
          "DATA path should revert to default after removing custom config");
    }

    @Test
    void setOnlyAffectsConfiguredStoreTypes() throws IOException {
      SystemStorageLibImpl impl = createImpl();

      Scope storage = impl.scope("my-scope");
      Storage configManager = storage.storage(StoreType.CONFIG);
      Path originalConfigPath = configManager.getDirPath();

      // Set custom dir only for DATA
      Path customDataDir = tempDir.resolve("custom-data");

      impl.metaConfig()
          .update(
              config ->
                  config.scope("my-scope").getCustomDirs().put(StoreType.DATA, customDataDir));

      // CONFIG path should be unchanged
      assertEquals(
          originalConfigPath,
          configManager.getDirPath(),
          "CONFIG path should not change when only DATA has custom dir");
    }

    @Test
    void setForDifferentScopeDoesNotAffectCurrentScope() throws IOException {
      SystemStorageLibImpl impl = createImpl();

      Scope storage = impl.scope("my-scope");
      Storage dataManager = storage.storage(StoreType.DATA);
      Path originalPath = dataManager.getDirPath();

      // Set a config change for a different scope
      Path customDataDir = tempDir.resolve("custom-data");

      impl.metaConfig()
          .update(
              config ->
                  config.scope("other-scope").getCustomDirs().put(StoreType.DATA, customDataDir));

      // my-scope's path should be unchanged
      assertEquals(
          originalPath,
          dataManager.getDirPath(),
          "DATA path for my-scope should not change when a different scope is configured");
    }

    @Test
    void setDoesNotUpdateNonCustomizableStoreTypes() {
      // Try to set a custom dir for CREDENTIALS (non-customizable)
      Path customCredentialsDir = tempDir.resolve("custom-credentials");

      assertThrows(
          IllegalArgumentException.class,
          () ->
              store
                  .get()
                  .scope("my-scope")
                  .getCustomDirs()
                  .put(StoreType.CREDENTIALS, customCredentialsDir));
    }
  }
}
