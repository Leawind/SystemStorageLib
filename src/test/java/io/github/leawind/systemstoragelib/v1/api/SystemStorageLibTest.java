package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class SystemStorageLibTest extends BaseTest {

  @Test
  void test() {
    ScopeStorage storage = lib.scope("system_storage_lib_test");
    assertNotNull(storage);
    assertEquals("system_storage_lib_test", storage.scope());
  }

  @Test
  void test2() throws IOException {
    var scope = lib.scope("system_storage_lib_test");
    var config = lib.metaConfig();

    config.get().getOrCreateScopeConfig(scope.scope());
  }
}
