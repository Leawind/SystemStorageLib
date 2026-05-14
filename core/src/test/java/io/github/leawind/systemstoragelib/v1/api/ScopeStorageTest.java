package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ScopeStorageTest extends SystemStorageLibTest {
  @Test
  void notNull() {
    assertNotNull(SCOPE.data());
    assertNotNull(SCOPE.config());
    assertNotNull(SCOPE.cache());
    assertNotNull(SCOPE.credentials());
    assertNotNull(SCOPE.logger());
  }

  @Test
  void testLock() throws IOException {
    StorageManager data = SCOPE.data();

    data.getLock().readLock().lock();

    data.getLock().readLock().unlock();
  }
}
