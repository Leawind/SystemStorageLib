package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import io.github.leawind.systemstoragelib.v1.impl.ScopeStorageImpl;
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

  private static final Logger TEST_LOGGER = LoggerFactory.getLogger(ScopeStorageTest.class);

  private ScopeStorage storage;

  @BeforeEach
  void setupEach() {
    dirs = new HashMap<>();
    dirs.put(StoreType.CREDENTIALS, tempDir.resolve("credentials"));
    dirs.put(StoreType.CONFIG, tempDir.resolve("config"));
    dirs.put(StoreType.DATA, tempDir.resolve("data"));
    dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
    dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));

    storage = new ScopeStorageImpl("test_scope", TEST_LOGGER, dirs);
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
  void ofCreatesScopedPaths() {
    ScopeStorage storage = new ScopeStorageImpl("my_scope", TEST_LOGGER, dirs);
    assertNotNull(storage);
    assertEquals("my_scope", storage.scope());

    StorageManager config = storage.storage(StoreType.CONFIG);
    assertTrue(config.getDirPath().endsWith("my_scope"));
  }

  @Test
  void throwsWhenMissingStoreTypes() {
    for (StoreType<?> type : StoreType.values()) {
      Map<StoreType<?>, Path> incomplete = new HashMap<>(dirs);
      incomplete.remove(type);
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ScopeStorageImpl("scope", TEST_LOGGER, incomplete));
      assertTrue(ex.getMessage().contains("Missing StoreTypes"));
    }
  }

  @Test
  void throwsWhenTwoStoreTypesShareSameDirPath() {
    Path sharedPath = tempDir.resolve("shared");
    Map<StoreType<?>, Path> duplicateDirs = new HashMap<>();
    duplicateDirs.put(StoreType.CREDENTIALS, sharedPath);
    duplicateDirs.put(StoreType.CONFIG, sharedPath);
    duplicateDirs.put(StoreType.DATA, tempDir.resolve("data"));
    duplicateDirs.put(StoreType.CACHE, tempDir.resolve("cache"));
    duplicateDirs.put(StoreType.DATA_LOCAL, tempDir.resolve("dl"));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ScopeStorageImpl("scope", TEST_LOGGER, duplicateDirs));
    assertTrue(ex.getMessage().contains("dirPath for each StoreType must be unique"));
  }
}
