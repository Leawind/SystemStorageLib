package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class SystemStorageLibTest extends BaseTest {

  @Test
  void test() {
    Scope scope = lib.scope("system_storage_lib_test");
    assertNotNull(scope);
    assertEquals("system_storage_lib_test", scope.name());
  }

  @Test
  void test2() throws IOException {
    var scope = lib.scope("system_storage_lib_test");

    MetaConfig config = lib.metaConfig().get();
    config.scope(scope.name()).getCustomDirs().put(StoreType.DATA, tempDir.resolve("/custom_data"));
    lib.metaConfig().set(config);
  }
}
