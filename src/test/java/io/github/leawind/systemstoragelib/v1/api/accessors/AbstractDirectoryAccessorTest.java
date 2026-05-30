package io.github.leawind.systemstoragelib.v1.api.accessors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.DirectoryAccessor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractDirectoryAccessorTest extends BaseTest {

  private DirectoryAccessor accessor;

  @BeforeEach
  void setupEach() {
    accessor = new AbstractDirectoryAccessor(tempDir.resolve("storage"), lib.logger()) {};
  }

  @Test
  void testGetDirPathReturnsProvidedPath() {
    assertEquals(tempDir.resolve("storage"), accessor.getDirPath());
  }

  @Test
  void testDirPathIsAbsoluteAndNormalized() {
    Path relativePath = tempDir.resolve("storage/../storage");
    var storage = new AbstractDirectoryAccessor(relativePath, lib.logger());
    assertEquals(relativePath.toAbsolutePath().normalize(), storage.getDirPath());
  }

  @Test
  void testSetDirPathUpdatesPath() {
    Path newPath = tempDir.resolve("new-storage");
    accessor.setDirPath(newPath);
    assertEquals(newPath, accessor.getDirPath());
  }

  @Test
  void testSetDirPathNormalizesPath() {
    Path newPath = tempDir.resolve("new-storage/../new-storage");
    accessor.setDirPath(newPath);
    assertEquals(newPath.toAbsolutePath().normalize(), accessor.getDirPath());
  }

  @Test
  void testDirCreatedOnGetLock() {
    assertFalse(Files.exists(accessor.getDirPath()));
  }
}
