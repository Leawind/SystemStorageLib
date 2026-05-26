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
import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import io.github.leawind.systemstoragelib.v1.api.stores.MetaConfigManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SystemStorageLibImplTest extends BaseTest {
  private MetaConfigManager manager;

  @BeforeEach
  void setupEach() {
    manager = lib.metaConfig();
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
      MetaConfigManager mgr = impl.metaConfig();
      assertNotNull(mgr);
    }
  }

  @Nested
  class CustomDirsFromMetaConfig {

    @Test
    void scopeUsesCustomDirsWhenConfigured() throws IOException {
      SystemStorageLibImpl impl = createImpl();

      // Set up custom dir for DATA store type in scope "my-scope"
      Path customDataDir = tempDir.resolve("custom-data");
      MetaConfig config = manager.createConfig();
      ScopeMetaConfig scopeMetaConfig =
          config.scopes().computeIfAbsent("my-scope", ignored -> config.createScopeConfig());
      scopeMetaConfig.getCustomDirs().put(StoreType.DATA, customDataDir);
      impl.metaConfig().set(config);

      // Create scope — it should use the custom dir for DATA
      Scope storage = impl.scope("my-scope");
      Storage dataManager = storage.storage(StoreType.DATA);

      assertEquals(
          customDataDir.resolve("my-scope"),
          dataManager.getDirPath(),
          "DATA storage should use the custom dir from MetaConfig");
    }

    @Test
    void scopeUsesDefaultDirsWhenNoCustomConfig() {
      SystemStorageLibImpl impl = createImpl();
      Scope storage = impl.scope("my-scope");
      Storage dataManager = storage.storage(StoreType.DATA);

      // Should fall back to defaultScopedDirs
      assertEquals(
          tempDir.resolve("data").resolve("my-scope"),
          dataManager.getDirPath(),
          "DATA storage should use the default dir when no custom config");
    }

    @Test
    void unconfiguredStoreTypesUseDefault() throws IOException {
      SystemStorageLibImpl impl = createImpl();

      // Only configure custom dir for DATA
      Path customDataDir = tempDir.resolve("custom-data");
      MetaConfig config = manager.createConfig();
      ScopeMetaConfig scopeMetaConfig =
          config.scopes().computeIfAbsent("my-scope", ignored -> config.createScopeConfig());
      scopeMetaConfig.getCustomDirs().put(StoreType.DATA, customDataDir);
      impl.metaConfig().set(config);

      Scope storage = impl.scope("my-scope");
      Storage configManager = storage.storage(StoreType.CONFIG);

      // CONFIG should still use default
      assertEquals(
          tempDir.resolve("config").resolve("my-scope"),
          configManager.getDirPath(),
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
      MetaConfig newConfig = manager.createConfig();
      ScopeMetaConfig scopeMetaConfig =
          newConfig.scopes().computeIfAbsent("my-scope", ignored -> newConfig.createScopeConfig());
      scopeMetaConfig.getCustomDirs().put(StoreType.DATA, customDataDir);
      impl.metaConfig().set(newConfig);

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
      MetaConfig customConfig = manager.createConfig();
      ScopeMetaConfig scopeMetaConfig =
          customConfig
              .scopes()
              .computeIfAbsent("my-scope", ignored -> customConfig.createScopeConfig());
      scopeMetaConfig.getCustomDirs().put(StoreType.DATA, customDataDir);

      Scope storage = impl.scope("my-scope");
      Storage dataManager = storage.storage(StoreType.DATA);

      impl.metaConfig().set(customConfig);

      assertEquals(
          customDataDir,
          dataManager.getDirPath(),
          "DATA path should use custom dir after first update");

      // Now set a config without the custom dir
      MetaConfig defaultConfig = manager.createConfig();
      impl.metaConfig().set(defaultConfig);

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
      MetaConfig newConfig = manager.createConfig();
      ScopeMetaConfig scopeMetaConfig =
          newConfig.scopes().computeIfAbsent("my-scope", ignored -> newConfig.createScopeConfig());
      scopeMetaConfig.getCustomDirs().put(StoreType.DATA, customDataDir);

      impl.metaConfig().set(newConfig);

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
      MetaConfig newConfig = manager.createConfig();
      ScopeMetaConfig scopeMetaConfig =
          newConfig
              .scopes()
              .computeIfAbsent("other-scope", ignored -> newConfig.createScopeConfig());
      scopeMetaConfig.getCustomDirs().put(StoreType.DATA, customDataDir);

      impl.metaConfig().set(newConfig);

      // my-scope's path should be unchanged
      assertEquals(
          originalPath,
          dataManager.getDirPath(),
          "DATA path for my-scope should not change when a different scope is configured");
    }

    @Test
    void setDoesNotUpdateNonCustomizableStoreTypes() {
      SystemStorageLibImpl impl = createImpl();

      // Create scope with default dirs
      Scope storage = impl.scope("my-scope");
      Path originalCredentialsPath = storage.storage(StoreType.CREDENTIALS).getDirPath();

      // Try to set a custom dir for CREDENTIALS (non-customizable)
      Path customCredentialsDir = tempDir.resolve("custom-credentials");
      MetaConfig config = manager.createConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            ScopeMetaConfig scopeMetaConfig =
                config.scopes().computeIfAbsent("my-scope", ignored -> config.createScopeConfig());
            scopeMetaConfig.getCustomDirs().put(StoreType.CREDENTIALS, customCredentialsDir);
          });
    }
  }
}
