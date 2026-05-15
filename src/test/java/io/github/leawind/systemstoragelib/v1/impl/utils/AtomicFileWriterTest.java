package io.github.leawind.systemstoragelib.v1.impl.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AtomicFileWriterTest {

  @TempDir Path tempDir;

  // region write()

  @Test
  void write_createsTargetFileWithData() throws IOException {
    Path target = tempDir.resolve("data.bin");
    byte[] data = "hello world".getBytes();

    AtomicFileWriter.write(target, data);

    assertTrue(Files.exists(target));
    assertArrayEquals(data, Files.readAllBytes(target));
  }

  @Test
  void write_createsParentDirectories() throws IOException {
    Path target = tempDir.resolve("a/b/c/data.bin");
    byte[] data = new byte[] {1, 2, 3};

    AtomicFileWriter.write(target, data);

    assertTrue(Files.exists(target));
    assertArrayEquals(data, Files.readAllBytes(target));
  }

  @Test
  void write_overwritesExistingFile() throws IOException {
    Path target = tempDir.resolve("data.bin");
    Files.writeString(target, "old content");

    byte[] newData = "new content".getBytes();
    AtomicFileWriter.write(target, newData);

    assertArrayEquals(newData, Files.readAllBytes(target));
  }

  @Test
  void write_noTmpFileRemainsAfterSuccess() throws IOException {
    Path target = tempDir.resolve("data.bin");
    AtomicFileWriter.write(target, new byte[] {1});

    try (var paths = Files.list(tempDir)) {
      long tmpCount = paths.filter(p -> p.toString().endsWith(".tmp")).count();
      assertEquals(0, tmpCount, "No .tmp file should remain after successful write");
    }
  }

  @Test
  void write_emptyData() throws IOException {
    Path target = tempDir.resolve("empty.bin");
    AtomicFileWriter.write(target, new byte[0]);

    assertTrue(Files.exists(target));
    assertArrayEquals(new byte[0], Files.readAllBytes(target));
  }

  @Test
  void write_largeData() throws IOException {
    Path target = tempDir.resolve("large.bin");
    byte[] data = new byte[1024 * 64];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i & 0xFF);
    }

    AtomicFileWriter.write(target, data);

    assertArrayEquals(data, Files.readAllBytes(target));
  }

  @Test
  void write_throwsIOException_whenTargetDirIsFile() throws IOException {
    Path blocker = tempDir.resolve("blocker");
    Files.writeString(blocker, "I am a file");
    Path target = blocker.resolve("data.bin");

    assertThrows(IOException.class, () -> AtomicFileWriter.write(target, new byte[] {1}));
  }

  // endregion

  // region resolveTmpPath()

  @Test
  void resolveTmpPath_appendsTmpSuffix() {
    Path target = Path.of("/some/dir/file.enc");
    Path tmp = AtomicFileWriter.resolveTmpPath(target);

    assertEquals(Path.of("/some/dir/file.enc.tmp"), tmp);
  }

  @Test
  void resolveTmpPath_sameDirectoryAsTarget() {
    Path target = Path.of("/a/b/c.txt");
    Path tmp = AtomicFileWriter.resolveTmpPath(target);

    assertEquals(target.getParent(), tmp.getParent());
  }

  // endregion

  // region writeToTmp()

  @Test
  void writeToTmp_createsFileWithData() throws IOException {
    Path tmp = tempDir.resolve("test.tmp");
    byte[] data = "temp data".getBytes();

    AtomicFileWriter.writeToTmp(tmp, data);

    assertTrue(Files.exists(tmp));
    assertArrayEquals(data, Files.readAllBytes(tmp));
  }

  @Test
  void writeToTmp_overwritesExistingTmp() throws IOException {
    Path tmp = tempDir.resolve("test.tmp");
    Files.writeString(tmp, "old tmp");

    byte[] newData = "new tmp".getBytes();
    AtomicFileWriter.writeToTmp(tmp, newData);

    assertArrayEquals(newData, Files.readAllBytes(tmp));
  }

  // endregion

  // region moveAtomically()

  @Test
  void moveAtomically_movesFileToTarget() throws IOException {
    Path source = tempDir.resolve("source.tmp");
    Path target = tempDir.resolve("target.bin");
    byte[] data = "moved data".getBytes();
    Files.write(source, data);

    AtomicFileWriter.moveAtomically(source, target);

    assertFalse(Files.exists(source), "Source file should be gone after move");
    assertTrue(Files.exists(target));
    assertArrayEquals(data, Files.readAllBytes(target));
  }

  @Test
  void moveAtomically_replacesExistingTarget() throws IOException {
    Path source = tempDir.resolve("source.tmp");
    Path target = tempDir.resolve("target.bin");
    Files.write(source, "new".getBytes());
    Files.write(target, "old".getBytes());

    AtomicFileWriter.moveAtomically(source, target);

    assertArrayEquals("new".getBytes(), Files.readAllBytes(target));
  }

  // endregion

  // region cleanUp()

  @Test
  void cleanUp_deletesExistingFile() throws IOException {
    Path tmp = tempDir.resolve("junk.tmp");
    Files.writeString(tmp, "junk");

    AtomicFileWriter.cleanUp(tmp);

    assertFalse(Files.exists(tmp));
  }

  @Test
  void cleanUp_doesNotThrowWhenFileAbsent() {
    Path tmp = tempDir.resolve("nonexistent.tmp");

    // Should not throw
    AtomicFileWriter.cleanUp(tmp);
  }

  // endregion

  // region Integration: failure scenario

  @Test
  void write_tmpCleanedUpOnFailure() throws IOException {
    Path targetDir = tempDir.resolve("target_is_dir");
    Files.createDirectories(targetDir);

    assertThrows(IOException.class, () -> AtomicFileWriter.write(targetDir, new byte[] {1}));

    // No .tmp file should remain
    try (var paths = Files.list(tempDir)) {
      long tmpCount =
          paths
              .filter(p -> p.toString().endsWith(".tmp") || p.toString().endsWith(".tmp.tmp"))
              .count();
      assertEquals(0, tmpCount, "No .tmp file should remain after failure");
    }
  }

  // endregion
}
