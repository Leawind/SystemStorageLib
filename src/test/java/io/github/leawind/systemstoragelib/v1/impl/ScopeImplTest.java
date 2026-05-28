package io.github.leawind.systemstoragelib.v1.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.Storage;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopeImplTest extends BaseTest {
  private Map<StoreType, Path> dirs;

  private static final Logger TEST_LOGGER = LoggerFactory.getLogger(ScopeImplTest.class);

  @BeforeEach
  void setupEach() {
    dirs = new HashMap<>();
    dirs.put(StoreType.CREDENTIALS, fs.getPath("credentials"));
    dirs.put(StoreType.CONFIG, fs.getPath("config"));
    dirs.put(StoreType.DATA, fs.getPath("data"));
    dirs.put(StoreType.CACHE, fs.getPath("cache"));
    dirs.put(StoreType.DATA_LOCAL, fs.getPath("data_local"));
  }

  @Test
  void notNull() {
    Scope storage = new ScopeImpl("test_scope", TEST_LOGGER, dirs);

    assertNotNull(storage.logger());
    assertNotNull(storage.storage(StoreType.CACHE));
    assertNotNull(storage.storage(StoreType.CONFIG));
    assertNotNull(storage.storage(StoreType.CREDENTIALS));
    assertNotNull(storage.storage(StoreType.DATA));
    assertNotNull(storage.storage(StoreType.DATA_LOCAL));

    assertEquals("test_scope", storage.name());
  }

  @Test
  void ofCreatesScopedPaths() {
    Scope scope = lib.scope("my_scope");
    assertNotNull(scope);
    assertEquals("my_scope", scope.name());

    Storage config = scope.storage(StoreType.CONFIG);
    assertTrue(config.getDirPath().endsWith("my_scope"));
  }

  @Test
  void throwsWhenMissingStoreTypes() {
    for (StoreType type : StoreType.values()) {
      Map<StoreType, Path> incomplete = new HashMap<>(dirs);
      incomplete.remove(type);

      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ScopeImpl("scope", TEST_LOGGER, incomplete));
      assertTrue(ex.getMessage().contains("Missing StoreTypes"));
    }
  }

  @Test
  void throwsWhenTwoStoreTypesShareSameDirPath() {
    Path sharedPath = fs.getPath("shared");
    Map<StoreType, Path> duplicateDirs = new HashMap<>();
    duplicateDirs.put(StoreType.CREDENTIALS, sharedPath);
    duplicateDirs.put(StoreType.CONFIG, sharedPath);
    duplicateDirs.put(StoreType.DATA, fs.getPath("data"));
    duplicateDirs.put(StoreType.CACHE, fs.getPath("cache"));
    duplicateDirs.put(StoreType.DATA_LOCAL, fs.getPath("dl"));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ScopeImpl("scope", TEST_LOGGER, duplicateDirs));
  }
}
