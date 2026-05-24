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
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FOO);

      assertEquals(FOO, config.customDirs().get(StoreType.DATA));
    }

    @Test
    void overwritesExistingMapping() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FOO);
      config.setCustomDir(StoreType.DATA, BAR);

      assertEquals(BAR, config.customDirs().get(StoreType.DATA));
    }

    @Test
    void throwsForNonCustomizableStoreType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PerScopeConfig.getDefault().setCustomDir(StoreType.CREDENTIALS, FOO));
    }

    @Test
    void throwsForDuplicatePathWithAnotherStoreType() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FOO);

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setCustomDir(StoreType.CONFIG, FOO));
    }

    @Test
    void throwsForDuplicatePathAfterNormalization() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FS.getPath("/foo"));

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setCustomDir(StoreType.CONFIG, FS.getPath("/bar/../foo")));
    }

    @Test
    void throwsForNullPath() {
      assertThrows(
          NullPointerException.class,
          () -> PerScopeConfig.getDefault().setCustomDir(StoreType.DATA, null));
    }

    @Test
    void throwsForRelativePath() {
      Path relative = FS.getPath("relative/path");
      assertThrows(
          IllegalArgumentException.class,
          () -> PerScopeConfig.getDefault().setCustomDir(StoreType.DATA, relative));
    }

    @Test
    void normalizesPath() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FS.getPath("/foo/../foo"));
      assertEquals(FS.getPath("/foo"), config.customDirs().get(StoreType.DATA));
    }
  }

  @Nested
  class SetCustomDirs {

    @Test
    void storesMultipleCustomDirs() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, FOO);
      dirs.put(StoreType.CONFIG, BAR);

      config.setCustomDirs(dirs);

      assertEquals(2, config.customDirs().size());
      assertEquals(FOO, config.customDirs().get(StoreType.DATA));
      assertEquals(BAR, config.customDirs().get(StoreType.CONFIG));
    }

    @Test
    void overwritesExistingMappings() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FOO);
      config.setCustomDir(StoreType.CONFIG, BAR);

      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, BAR);
      config.setCustomDirs(dirs);

      assertEquals(1, config.customDirs().size());
      assertEquals(BAR, config.customDirs().get(StoreType.DATA));
    }

    @Test
    void replacesAllMappings() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FOO);

      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.CONFIG, BAR);
      config.setCustomDirs(dirs);

      assertEquals(1, config.customDirs().size());
      assertFalse(config.customDirs().containsKey(StoreType.DATA));
    }

    @Test
    void isAtomicOnFailure() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FOO);

      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.CONFIG, BAR);
      dirs.put(StoreType.CREDENTIALS, BAR); // not customizable

      assertThrows(IllegalArgumentException.class, () -> config.setCustomDirs(dirs));
      // DATA should retain its original value, CONFIG should not have been set
      assertEquals(FOO, config.customDirs().get(StoreType.DATA));
      assertFalse(config.customDirs().containsKey(StoreType.CONFIG));
    }

    @Test
    void throwsForDuplicatePaths() {
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, FOO);
      dirs.put(StoreType.CONFIG, FOO); // same path

      assertThrows(
          IllegalArgumentException.class,
          () -> PerScopeConfig.getDefault().setCustomDirs(dirs));
    }

    @Test
    void throwsForDuplicatePathsAfterNormalization() {
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, FS.getPath("/foo"));
      dirs.put(StoreType.CONFIG, FS.getPath("/bar/../foo")); // normalizes to same path

      assertThrows(
          IllegalArgumentException.class,
          () -> PerScopeConfig.getDefault().setCustomDirs(dirs));
    }

    @Test
    void throwsForNullPathInEntries() {
      Map<StoreType<?>, Path> dirs = new HashMap<>();
      dirs.put(StoreType.DATA, null);

      assertThrows(
          NullPointerException.class, () -> PerScopeConfig.getDefault().setCustomDirs(dirs));
    }
  }

  @Nested
  class UnsetCustomDir {

    @Test
    void removesMapping() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FOO);
      config.unsetCustomDir(StoreType.DATA);

      assertFalse(config.customDirs().containsKey(StoreType.DATA));
    }

    @Test
    void doesNothingForNonExistentMapping() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.unsetCustomDir(StoreType.DATA);

      assertTrue(config.customDirs().isEmpty());
    }
  }

  @Nested
  class ResetCustomDirs {

    @Test
    void clearsAllMappings() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      config.setCustomDir(StoreType.DATA, FOO);
      config.setCustomDir(StoreType.CONFIG, BAR);
      config.resetCustomDirs();

      assertTrue(config.customDirs().isEmpty());
    }
  }

  @Nested
  class CustomDirs {

    @Test
    void returnsUnmodifiableMap() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      assertThrows(
          UnsupportedOperationException.class, () -> config.customDirs().put(StoreType.DATA, FOO));
    }

    @Test
    void emptyByDefault() {
      assertTrue(PerScopeConfig.getDefault().customDirs().isEmpty());
    }
  }

  @Nested
  class Constructor {

    @Test
    void defensiveCopyPreventsExternalMutation() {
      Map<StoreType<?>, Path> mutableMap = new HashMap<>();
      mutableMap.put(StoreType.DATA, FOO);
      PerScopeConfig config = new PerScopeConfig(mutableMap);

      mutableMap.put(StoreType.DATA, BAR);

      assertEquals(FOO, config.customDirs().get(StoreType.DATA));
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void equalWhenCustomDirsEqual() {
      PerScopeConfig a = PerScopeConfig.getDefault();
      a.setCustomDir(StoreType.DATA, FOO);
      PerScopeConfig b = PerScopeConfig.getDefault();
      b.setCustomDir(StoreType.DATA, FOO);

      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualWhenCustomDirsDiffer() {
      PerScopeConfig a = PerScopeConfig.getDefault();
      a.setCustomDir(StoreType.DATA, FOO);
      PerScopeConfig b = PerScopeConfig.getDefault();
      b.setCustomDir(StoreType.DATA, BAR);

      assertNotEquals(a, b);
    }

    @Test
    void notEqualWhenOneHasMoreMappings() {
      PerScopeConfig a = PerScopeConfig.getDefault();
      a.setCustomDir(StoreType.DATA, FOO);
      PerScopeConfig b = PerScopeConfig.getDefault();

      assertNotEquals(a, b);
    }
  }

  @Nested
  class GetDefault {

    @Test
    void returnsConfigWithEmptyCustomDirs() {
      PerScopeConfig config = PerScopeConfig.getDefault();
      assertNotNull(config);
      assertTrue(config.customDirs().isEmpty());
    }

    @Test
    void isIdempotent() {
      assertEquals(PerScopeConfig.getDefault(), PerScopeConfig.getDefault());
    }
  }
}
