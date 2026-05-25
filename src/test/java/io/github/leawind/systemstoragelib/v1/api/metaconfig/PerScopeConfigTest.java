package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.impl.metaconfig.PerScopeConfigImpl;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PerScopeConfigTest {

  private static final FileSystem FS = Jimfs.newFileSystem(Configuration.unix());
  private static final Path FOO = FS.getPath("/foo");
  private static final Path BAR = FS.getPath("/bar");

  @Nested
  class SetCustomDir {

    @Test
    void storesAndRetrievesViaCustomDirs() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FOO);

      assertEquals(FOO, config.getCustomDirs().get(StoreType.DATA));
    }

    @Test
    void overwritesExistingMapping() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FOO);
      config.getCustomDirs().put(StoreType.DATA, BAR);

      assertEquals(BAR, config.getCustomDirs().get(StoreType.DATA));
    }

    @Test
    void throwsForNonCustomizableStoreType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            PerScopeConfig perScopeConfig = PerScopeConfig.createDefault();
            perScopeConfig.getCustomDirs().put(StoreType.CREDENTIALS, FOO);
          });
    }

    @Test
    void throwsForDuplicatePathWithAnotherStoreType() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FOO);

      assertThrows(
          IllegalArgumentException.class, () -> config.getCustomDirs().put(StoreType.CONFIG, FOO));
    }

    @Test
    void throwsForDuplicatePathAfterNormalization() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FS.getPath("/foo"));

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            Path path = FS.getPath("/bar/../foo");
            config.getCustomDirs().put(StoreType.CONFIG, path);
          });
    }

    @Test
    void throwsForNullPath() {
      assertThrows(
          NullPointerException.class,
          () -> {
            PerScopeConfig perScopeConfig = PerScopeConfig.createDefault();
            perScopeConfig.getCustomDirs().put(StoreType.DATA, null);
          });
    }

    @Test
    void throwsForRelativePath() {
      Path relative = FS.getPath("relative/path");
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            PerScopeConfig perScopeConfig = PerScopeConfig.createDefault();
            perScopeConfig.getCustomDirs().put(StoreType.DATA, relative);
          });
    }

    @Test
    void normalizesPath() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      Path path = FS.getPath("/foo/../foo");
      config.getCustomDirs().put(StoreType.DATA, path);
      assertEquals(FS.getPath("/foo"), config.getCustomDirs().get(StoreType.DATA));
    }
  }

  @Nested
  class SetCustomDirs {

    @Test
    void storesMultipleCustomDirs() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, FOO);
      dirs.put(StoreType.CONFIG, BAR);

      config.setCustomDirs(dirs);

      assertEquals(2, config.getCustomDirs().size());
      assertEquals(FOO, config.getCustomDirs().get(StoreType.DATA));
      assertEquals(BAR, config.getCustomDirs().get(StoreType.CONFIG));
    }

    @Test
    void overwritesExistingMappings() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FOO);
      config.getCustomDirs().put(StoreType.CONFIG, BAR);

      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, BAR);
      config.setCustomDirs(dirs);

      assertEquals(1, config.getCustomDirs().size());
      assertEquals(BAR, config.getCustomDirs().get(StoreType.DATA));
    }

    @Test
    void replacesAllMappings() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FOO);

      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.CONFIG, BAR);
      config.setCustomDirs(dirs);

      assertEquals(1, config.getCustomDirs().size());
      assertFalse(config.getCustomDirs().containsKey(StoreType.DATA));
    }

    @Test
    void isAtomicOnFailure() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FOO);

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setCustomDirs(Map.of(StoreType.CONFIG, BAR, StoreType.DATA_LOCAL, BAR)));

      // DATA should retain its original value, CONFIG should not have been set
      assertEquals(FOO, config.getCustomDirs().get(StoreType.DATA));
      assertFalse(config.getCustomDirs().containsKey(StoreType.CONFIG));
    }

    @Test
    void throwsForDuplicatePaths() {
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, FOO);
      dirs.put(StoreType.CONFIG, FOO); // same path

      assertThrows(
          IllegalArgumentException.class, () -> PerScopeConfig.createDefault().setCustomDirs(dirs));
    }

    @Test
    void throwsForDuplicatePathsAfterNormalization() {
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, FS.getPath("/foo"));
      dirs.put(StoreType.CONFIG, FS.getPath("/bar/../foo")); // normalizes to same path

      assertThrows(
          IllegalArgumentException.class, () -> PerScopeConfig.createDefault().setCustomDirs(dirs));
    }

    @Test
    void throwsForNullPathInEntries() {
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, null);

      assertThrows(
          NullPointerException.class, () -> PerScopeConfig.createDefault().setCustomDirs(dirs));
    }
  }

  @Nested
  class UnsetCustomDir {

    @Test
    void removesMapping() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FOO);
      config.getCustomDirs().remove(StoreType.DATA);

      assertFalse(config.getCustomDirs().containsKey(StoreType.DATA));
    }

    @Test
    void doesNothingForNonExistentMapping() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().remove(StoreType.DATA);

      assertTrue(config.getCustomDirs().isEmpty());
    }
  }

  @Nested
  class ResetCustomDirs {

    @Test
    void clearsAllMappings() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      config.getCustomDirs().put(StoreType.DATA, FOO);
      config.getCustomDirs().put(StoreType.CONFIG, BAR);
      config.getCustomDirs().clear();

      assertTrue(config.getCustomDirs().isEmpty());
    }
  }

  @Nested
  class CustomDirs {

    @Test
    void emptyByDefault() {
      assertTrue(PerScopeConfig.createDefault().getCustomDirs().isEmpty());
    }
  }

  @Nested
  class Constructor {

    @Test
    void defensiveCopyPreventsExternalMutation() {
      Map<StoreType<?>, Path> mutableMap = new HashMap<>();
      mutableMap.put(StoreType.DATA, FOO);
      PerScopeConfig config = new PerScopeConfigImpl(mutableMap);

      mutableMap.put(StoreType.DATA, BAR);

      assertEquals(FOO, config.getCustomDirs().get(StoreType.DATA));
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void equalWhenCustomDirsEqual() {
      PerScopeConfig a = PerScopeConfig.createDefault();
      a.getCustomDirs().put(StoreType.DATA, FOO);
      PerScopeConfig b = PerScopeConfig.createDefault();
      b.getCustomDirs().put(StoreType.DATA, FOO);

      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualWhenCustomDirsDiffer() {
      PerScopeConfig a = PerScopeConfig.createDefault();
      a.getCustomDirs().put(StoreType.DATA, FOO);
      PerScopeConfig b = PerScopeConfig.createDefault();
      b.getCustomDirs().put(StoreType.DATA, BAR);

      assertNotEquals(a, b);
    }

    @Test
    void notEqualWhenOneHasMoreMappings() {
      PerScopeConfig a = PerScopeConfig.createDefault();
      a.getCustomDirs().put(StoreType.DATA, FOO);
      PerScopeConfig b = PerScopeConfig.createDefault();

      assertNotEquals(a, b);
    }
  }

  @Nested
  class GetDefault {

    @Test
    void returnsConfigWithEmptyCustomDirs() {
      PerScopeConfig config = PerScopeConfig.createDefault();
      assertNotNull(config);
      assertTrue(config.getCustomDirs().isEmpty());
    }

    @Test
    void isIdempotent() {
      assertEquals(PerScopeConfig.createDefault(), PerScopeConfig.createDefault());
    }
  }
}
