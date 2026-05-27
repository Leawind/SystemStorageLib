package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class SystemStorageLibTest extends BaseTest {

  @Test
  void test() {
    Scope scope = lib.scope("example_mod");
    assertNotNull(scope);
    assertEquals("example_mod", scope.name());
  }

  @Test
  void test2() throws IOException {
    var scope = lib.scope("example_mod");

    lib.metaConfig()
        .update(
            config ->
                config
                    .scope(scope.name())
                    .getCustomDirs()
                    .put(StoreType.DATA, tempDir.resolve("/custom_data")));
  }
}
