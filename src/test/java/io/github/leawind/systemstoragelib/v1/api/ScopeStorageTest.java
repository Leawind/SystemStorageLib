package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopeStorageTest {
  @TempDir Path tempDir;
  private Map<StoreType<?>, Path> dirs;
  private Map<StoreType<?>, ? extends StorageManager> managers;

  private static final Logger TEST_LOGGER = LoggerFactory.getLogger(ScopeStorageTest.class);

  private ScopeStorage storage;

  @BeforeEach
  void setupEach() {
    {
      dirs = new HashMap<>();
      dirs.put(StoreType.CREDENTIALS, tempDir.resolve("credentials"));
      dirs.put(StoreType.CONFIG, tempDir.resolve("config"));
      dirs.put(StoreType.DATA, tempDir.resolve("data"));
      dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
      dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));
    }

    {
      Map<StoreType<?>, StorageManager> builder = new HashMap<>();
      builder.put(
          StoreType.CREDENTIALS, StoreType.CREDENTIALS.manager(dirs.get(StoreType.CREDENTIALS)));
      builder.put(StoreType.CONFIG, StoreType.CONFIG.manager(dirs.get(StoreType.CONFIG)));
      builder.put(StoreType.DATA, StoreType.DATA.manager(dirs.get(StoreType.DATA)));
      builder.put(StoreType.CACHE, StoreType.CACHE.manager(dirs.get(StoreType.CACHE)));
      builder.put(
          StoreType.DATA_LOCAL, StoreType.DATA_LOCAL.manager(dirs.get(StoreType.DATA_LOCAL)));
      managers = builder;
    }
    storage = ScopeStorage.ofDirs("test_scope", TEST_LOGGER, dirs);
  }

  @Test
  void notNull() {
    assertNotNull(storage.logger());
    assertNotNull(storage.storage(StoreType.CACHE));
    assertNotNull(storage.storage(StoreType.CONFIG));
    assertNotNull(storage.storage(StoreType.CREDENTIALS));
    assertNotNull(storage.storage(StoreType.DATA));
    assertNotNull(storage.storage(StoreType.DATA_LOCAL));

    assertEquals("test_scope", storage.scope());
  }

  @Test
  void ofDirsCreatesScopedPaths() {
    ScopeStorage storage = ScopeStorage.ofDirs("my_scope", TEST_LOGGER, dirs);
    assertNotNull(storage);
    assertEquals("my_scope", storage.scope());

    StorageManager config = storage.storage(StoreType.CONFIG);
    assertTrue(config.getDirPath().endsWith("my_scope"));
  }

  @Test
  void throwsWhenMissingStoreTypes() {
    for (var type : StoreType.values()) {
      {
        Map<StoreType<?>, Path> incomplete = new HashMap<>(dirs);
        incomplete.remove(type);
        var ex =
            assertThrows(
                IllegalArgumentException.class,
                () -> ScopeStorage.ofDirs("scope", TEST_LOGGER, incomplete));
        assertTrue(ex.getMessage().contains("Missing StoreTypes"));
      }
      {
        Map<StoreType<?>, ? extends StorageManager> incomplete = new HashMap<>(managers);
        incomplete.remove(type);
        var ex =
            assertThrows(
                IllegalArgumentException.class,
                () -> ScopeStorage.of("scope", TEST_LOGGER, incomplete));
        assertTrue(ex.getMessage().contains("Missing StoreTypes"));
      }
    }
  }

  @Test
  void throwsWhenCredentialsManagerIsWrongType() {
    Map<StoreType<?>, StorageManager> wrongType = new HashMap<>(managers);
    wrongType.put(StoreType.CREDENTIALS, StorageManager.of(tempDir.resolve("credentials")));
    var ex =
        assertThrows(
            IllegalArgumentException.class, () -> ScopeStorage.of("scope", TEST_LOGGER, wrongType));
    assertTrue(ex.getMessage().contains("Invalid manager type"));
  }

  @Test
  void throwsWhenTwoStoreTypesShareSameDirPath() {
    Path sharedPath = tempDir.resolve("shared");
    Map<StoreType<?>, StorageManager> duplicateDirs = new HashMap<>();
    duplicateDirs.put(StoreType.CREDENTIALS, StoreType.CREDENTIALS.manager(sharedPath));
    duplicateDirs.put(StoreType.CONFIG, StoreType.CONFIG.manager(sharedPath));
    duplicateDirs.put(StoreType.DATA, StoreType.DATA.manager(tempDir.resolve("data")));
    duplicateDirs.put(StoreType.CACHE, StoreType.CACHE.manager(tempDir.resolve("cache")));
    duplicateDirs.put(StoreType.DATA_LOCAL, StoreType.DATA_LOCAL.manager(tempDir.resolve("dl")));

    var ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ScopeStorage.of("scope", TEST_LOGGER, duplicateDirs));
    assertTrue(ex.getMessage().contains("dirPath for each StoreType must be unique"));
  }
}
