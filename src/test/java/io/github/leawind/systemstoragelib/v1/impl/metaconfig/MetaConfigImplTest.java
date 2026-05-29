package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MetaConfigImplTest extends BaseTest {

  @Nested
  class TestEquals {
    MetaConfigImpl a;
    MetaConfigImpl b;

    @BeforeEach
    void setup() {
      a = new MetaConfigImpl(lib, Collections.emptyMap(), 7355608, 8);
      b = new MetaConfigImpl(lib, Collections.emptyMap(), 7355608, 8);
    }

    @Test
    void shouldEqual() {
      assertEquals(a, b);
    }

    @Test
    void setCustomDirs() {
      a.scope("example_mod").getCustomDirs().put(StoreType.DATA, fs.getPath("/custom/data"));
      assertNotEquals(a, b);

      b.scope("example_mod").getCustomDirs().put(StoreType.DATA, fs.getPath("/custom/data"));
      assertEquals(a, b);
    }

    @Test
    void setMaxLogFileSize() {
      a.setMaxLogFileSize(12138);
      assertNotEquals(a, b);

      b.setMaxLogFileSize(12138);
      assertEquals(a, b);
    }
  }
}
