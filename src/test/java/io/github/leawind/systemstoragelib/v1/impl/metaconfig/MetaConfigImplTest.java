package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class MetaConfigImplTest extends BaseTest {
  @Test
  void testEquals() {
    var a = new MetaConfigImpl(lib, Collections.emptyMap());
    var b = new MetaConfigImpl(lib, Collections.emptyMap());

    assertEquals(a, b);

    a.scope("example_mod").getCustomDirs().put(StoreType.DATA, fs.getPath("/custom/data"));
    assertNotEquals(a, b);
    b.scope("example_mod").getCustomDirs().put(StoreType.DATA, fs.getPath("/custom/data"));
    assertEquals(a, b);
  }
}
