package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemStorageLibTest {
  public static final String SCOPE_ID = "system_storage_lib_test";

  public ScopeStorage storage;

  @BeforeEach
  void setupEach() {
    storage = SystemStorageLib.getInstance().scope(SCOPE_ID);
  }

  @Test
  void test() {
    assertNotNull(storage);
    assertEquals(SCOPE_ID, storage.scope());
  }
}
