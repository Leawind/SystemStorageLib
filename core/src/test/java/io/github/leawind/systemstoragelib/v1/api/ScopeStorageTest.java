package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ScopeStorageTest extends SystemStorageLibTest {
  @Test
  void notNull() {
    assertNotNull(storage.logger());
    assertNotNull(storage.credentials());
    assertNotNull(storage.config());
    assertNotNull(storage.data());
    assertNotNull(storage.dataLocal());
    assertNotNull(storage.cache());
  }
}
