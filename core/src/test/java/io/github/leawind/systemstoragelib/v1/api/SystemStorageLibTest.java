package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class SystemStorageLibTest {

  @Test
  void test() {
    SystemStorageLib lib = SystemStorageLib.getInstance();
    ScopeStorage storage = lib.scope("system_storage_lib_test");
    assertNotNull(storage);
    assertEquals("system_storage_lib_test", storage.scope());
  }
}
