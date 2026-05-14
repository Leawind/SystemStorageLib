package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ScopeStorageTest extends SystemStorageLibTest {
  @Test
  void notNull() {
    assertNotNull(storage.logger());
    assertNotNull(storage.storage(StoreType.CACHE));
    assertNotNull(storage.storage(StoreType.CONFIG));
    assertNotNull(storage.storage(StoreType.CREDENTIALS));
    assertNotNull(storage.storage(StoreType.DATA));
    assertNotNull(storage.storage(StoreType.DATA_LOCAL));
  }
}
