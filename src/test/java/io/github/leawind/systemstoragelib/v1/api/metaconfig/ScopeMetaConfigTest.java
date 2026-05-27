package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ScopeMetaConfigTest extends BaseTest {

  private ScopeMetaConfig config;
  private Path foo;
  private Path bar;

  @BeforeEach
  void setup() throws IOException {
    config = lib.metaConfig().get().scope("example_mod");
    foo = fs.getPath("/foo");
    bar = fs.getPath("/bar");
  }

  @Nested
  class SetCustomDir {

    @Test
    void storesAndRetrievesViaCustomDirs() {
      config.getCustomDirs().put(StoreType.DATA, foo);

      assertEquals(foo, config.getCustomDirs().get(StoreType.DATA));
    }

    @Test
    void overwritesExistingMapping() {
      config.getCustomDirs().put(StoreType.DATA, foo);
      config.getCustomDirs().put(StoreType.DATA, bar);

      assertEquals(bar, config.getCustomDirs().get(StoreType.DATA));
    }

    @Test
    void throwsForNonCustomizableStoreType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> config.getCustomDirs().put(StoreType.CREDENTIALS, foo));
    }

    @Test
    void throwsForDuplicatePathWithAnotherStoreType() {
      config.getCustomDirs().put(StoreType.DATA, foo);

      assertThrows(
          IllegalArgumentException.class, () -> config.getCustomDirs().put(StoreType.CONFIG, foo));
    }

    @Test
    void throwsForDuplicatePathAfterNormalization() {
      config.getCustomDirs().put(StoreType.DATA, fs.getPath("/foo"));

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            Path path = fs.getPath("/bar/../foo");
            config.getCustomDirs().put(StoreType.CONFIG, path);
          });
    }

    @Test
    void throwsForNullPath() {
      assertThrows(
          NullPointerException.class, () -> config.getCustomDirs().put(StoreType.DATA, null));
    }

    @Test
    void throwsForRelativePath() {
      Path relative = fs.getPath("relative/path");
      assertThrows(
          IllegalArgumentException.class,
          () -> config.getCustomDirs().put(StoreType.DATA, relative));
    }

    @Test
    void normalizesPath() {
      Path path = fs.getPath("/foo/../foo");
      config.getCustomDirs().put(StoreType.DATA, path);
      assertEquals(fs.getPath("/foo"), config.getCustomDirs().get(StoreType.DATA));
    }
  }

  @Nested
  class SetCustomDirs {

    @Test
    void storesMultipleCustomDirs() {
      Map<StoreType, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, foo);
      dirs.put(StoreType.CONFIG, bar);

      config.setCustomDirs(dirs);

      assertEquals(2, config.getCustomDirs().size());
      assertEquals(foo, config.getCustomDirs().get(StoreType.DATA));
      assertEquals(bar, config.getCustomDirs().get(StoreType.CONFIG));
    }

    @Test
    void overwritesExistingMappings() {
      config.getCustomDirs().put(StoreType.DATA, foo);
      config.getCustomDirs().put(StoreType.CONFIG, bar);

      Map<StoreType, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, bar);
      config.setCustomDirs(dirs);

      assertEquals(1, config.getCustomDirs().size());
      assertEquals(bar, config.getCustomDirs().get(StoreType.DATA));
    }

    @Test
    void replacesAllMappings() {
      config.getCustomDirs().put(StoreType.DATA, foo);

      Map<StoreType, Path> dirs = new HashMap<>();
      dirs.put(StoreType.CONFIG, bar);
      config.setCustomDirs(dirs);

      assertEquals(1, config.getCustomDirs().size());
      assertFalse(config.getCustomDirs().containsKey(StoreType.DATA));
    }

    @Test
    void isAtomicOnFailure() {
      config.getCustomDirs().put(StoreType.DATA, foo);

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setCustomDirs(Map.of(StoreType.CONFIG, bar, StoreType.DATA_LOCAL, bar)));

      // DATA should retain its original value, CONFIG should not have been set
      assertEquals(foo, config.getCustomDirs().get(StoreType.DATA));
      assertFalse(config.getCustomDirs().containsKey(StoreType.CONFIG));
    }

    @Test
    void throwsForDuplicatePaths() {
      Map<StoreType, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, foo);
      dirs.put(StoreType.CONFIG, foo); // same path

      assertThrows(IllegalArgumentException.class, () -> config.setCustomDirs(dirs));
    }

    @Test
    void throwsForDuplicatePathsAfterNormalization() {
      Map<StoreType, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, fs.getPath("/foo"));
      dirs.put(StoreType.CONFIG, fs.getPath("/bar/../foo")); // normalizes to same path

      assertThrows(IllegalArgumentException.class, () -> config.setCustomDirs(dirs));
    }

    @Test
    void throwsForNullPathInEntries() {
      Map<StoreType, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, null);

      assertThrows(NullPointerException.class, () -> config.setCustomDirs(dirs));
    }
  }

  @Nested
  class UnsetCustomDir {

    @Test
    void removesMapping() {
      config.getCustomDirs().put(StoreType.DATA, foo);
      config.getCustomDirs().remove(StoreType.DATA);

      assertFalse(config.getCustomDirs().containsKey(StoreType.DATA));
    }

    @Test
    void doesNothingForNonExistentMapping() {
      config.getCustomDirs().remove(StoreType.DATA);

      assertTrue(config.getCustomDirs().isEmpty());
    }
  }

  @Nested
  class ResetCustomDirs {

    @Test
    void clearsAllMappings() {
      config.getCustomDirs().put(StoreType.DATA, foo);
      config.getCustomDirs().put(StoreType.CONFIG, bar);
      config.getCustomDirs().clear();

      assertTrue(config.getCustomDirs().isEmpty());
    }
  }

  @Nested
  class CustomDirs {

    @Test
    void emptyByDefault() {
      assertTrue(config.getCustomDirs().isEmpty());
    }
  }
}
