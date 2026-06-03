package io.github.leawind.systemstoragelib.v1.api.dirdoc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectoryDocumenterTest {

  private static final String README_NAME = "README.md";
  private static final String README_CONTENT = "# Managed Directory\nDo not delete manually.";

  @TempDir Path tempDir;
  private DirectoryDocumenter.Mutable directoryDocumenter;

  @BeforeEach
  void setUp() {
    directoryDocumenter = DirectoryDocumenter.mutable(README_NAME);
  }

  @Test
  void memorize_and_remember() {
    Path path = tempDir.resolve("test-dir");
    assertFalse(directoryDocumenter.remember(path));

    directoryDocumenter.memorize(path, README_CONTENT);
    assertTrue(directoryDocumenter.remember(path));

    // Normalized path should match
    assertTrue(directoryDocumenter.remember(path.toAbsolutePath().normalize()));
  }

  @Test
  void memorize_null_path_throws() {
    assertThrows(
        NullPointerException.class, () -> directoryDocumenter.memorize(null, README_CONTENT));
  }

  @Test
  void memorize_null_content_throws() {
    Path path = tempDir.resolve("test");
    assertThrows(NullPointerException.class, () -> directoryDocumenter.memorize(path, null));
  }

  @Test
  void forget_single_path() {
    Path path = tempDir.resolve("test-dir");
    directoryDocumenter.memorize(path, README_CONTENT);
    assertTrue(directoryDocumenter.remember(path));

    directoryDocumenter.forget(path);
    assertFalse(directoryDocumenter.remember(path));
  }

  @Test
  void forget_unmemorized_path_does_nothing() {
    Path path = tempDir.resolve("unknown");
    assertDoesNotThrow(() -> directoryDocumenter.forget(path));
    assertFalse(directoryDocumenter.remember(path));
  }

  @Test
  void forget_all_clears_entries() {
    Path p1 = tempDir.resolve("dir1");
    Path p2 = tempDir.resolve("dir2");
    directoryDocumenter.memorize(p1, "content1");
    directoryDocumenter.memorize(p2, "content2");

    assertTrue(directoryDocumenter.remember(p1));
    assertTrue(directoryDocumenter.remember(p2));

    directoryDocumenter.forgetAll();
    assertFalse(directoryDocumenter.remember(p1));
    assertFalse(directoryDocumenter.remember(p2));
  }

  @Test
  void properties() throws IOException {
    directoryDocumenter.memorize(tempDir.resolve("dir"), "Hello, ${name}");

    directoryDocumenter.properties().put("name", "Steve");
    directoryDocumenter.createDirectories(tempDir.resolve("dir"));
    assertEquals("Hello, Steve", Files.readString(tempDir.resolve("dir").resolve(README_NAME)));
  }

  @Test
  void create_Directories_directory_with_readme() throws IOException {
    Path target = tempDir.resolve("new-dir");
    directoryDocumenter.memorize(target, README_CONTENT);

    directoryDocumenter.createDirectories(target);

    assertTrue(Files.isDirectory(target));
    Path readme = target.resolve(README_NAME);
    assertTrue(Files.isRegularFile(readme));
    assertEquals(README_CONTENT, Files.readString(readme));
  }

  @Test
  void create_Directories_nested_directories_with_readme() throws IOException {
    Path target = tempDir.resolve("a").resolve("b").resolve("c");
    directoryDocumenter.memorize(tempDir.resolve("a"), "# Level A");
    directoryDocumenter.memorize(tempDir.resolve("a").resolve("b"), "# Level B");
    directoryDocumenter.memorize(target, "# Level C");

    directoryDocumenter.createDirectories(target);

    assertTrue(Files.isDirectory(target));
    assertReadmeExists(tempDir.resolve("a"), "# Level A");
    assertReadmeExists(tempDir.resolve("a").resolve("b"), "# Level B");
    assertReadmeExists(target, "# Level C");
  }

  @Test
  void create_Directories_existing_directory_checks_readme() throws IOException {
    Path target = tempDir.resolve("existing");
    Files.createDirectory(target);
    directoryDocumenter.memorize(target, README_CONTENT);

    directoryDocumenter.createDirectories(target);

    Path readme = target.resolve(README_NAME);
    assertTrue(Files.isRegularFile(readme));
    assertEquals(README_CONTENT, Files.readString(readme));
  }

  @Test
  void create_Directories_does_not_overwrite_existing_readme() throws IOException {
    Path target = tempDir.resolve("dir");
    directoryDocumenter.memorize(target, README_CONTENT);
    directoryDocumenter.createDirectories(target);

    String modified = "# Modified\nDo not touch.";
    Files.writeString(target.resolve(README_NAME), modified);

    directoryDocumenter.createDirectories(target);

    assertEquals(modified, Files.readString(target.resolve(README_NAME)));
  }

  @Test
  void create_Directories_with_file_attributes() throws IOException {
    Path target = tempDir.resolve("attrs-dir");
    directoryDocumenter.memorize(target, README_CONTENT);

    directoryDocumenter.createDirectories(target);

    assertTrue(Files.isDirectory(target));
    assertTrue(Files.isRegularFile(target.resolve(README_NAME)));
  }

  @Test
  void create_Directories_null_path_throws() {
    assertThrows(NullPointerException.class, () -> directoryDocumenter.createDirectories(null));
  }

  @Test
  void memorizeByResource_success() {
    Path path = tempDir.resolve("resource-dir");
    assertDoesNotThrow(() -> directoryDocumenter.memorizeByResource(path, "/sample-readme.txt"));
    assertTrue(directoryDocumenter.remember(path));
  }

  @Test
  void memorizeByResource_missing_resource_throws() {
    Path path = tempDir.resolve("bad-resource");
    assertThrows(
        RuntimeException.class,
        () -> directoryDocumenter.memorizeByResource(path, "nonexistent.txt"));
  }

  @Test
  void path_normalization_ensures_consistency() {
    Path raw = tempDir.resolve("dir");
    Path absolute = raw.toAbsolutePath();
    Path normalized = absolute.normalize();

    directoryDocumenter.memorize(raw, README_CONTENT);
    assertTrue(directoryDocumenter.remember(absolute));
    assertTrue(directoryDocumenter.remember(normalized));

    directoryDocumenter.forget(absolute);
    assertFalse(directoryDocumenter.remember(raw));
  }

  @Test
  void concurrent_memorize_and_remember() throws InterruptedException {
    int threads = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    AtomicBoolean failed = new AtomicBoolean(false);

    for (int i = 0; i < threads; i++) {
      final int idx = i;
      executor.submit(
          () -> {
            try {
              Path p = tempDir.resolve("concurrent-" + idx);
              directoryDocumenter.memorize(p, "content-" + idx);
              if (!directoryDocumenter.remember(p)) {
                failed.set(true);
              }
            } catch (Exception e) {
              failed.set(true);
            } finally {
              latch.countDown();
            }
          });
    }

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    executor.shutdown();
    assertFalse(failed.get(), "Concurrent access should not cause failures");
  }

  @Test
  void concurrent_create_Directories_with_readme() throws InterruptedException, IOException {
    int threads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);

    for (int i = 0; i < threads; i++) {
      final int idx = i;
      executor.submit(
          () -> {
            try {
              Path p = tempDir.resolve("thread-dir-" + idx);
              directoryDocumenter.memorize(p, "Readme for " + idx);
              directoryDocumenter.createDirectories(p);
              assertTrue(Files.isRegularFile(p.resolve(README_NAME)));
            } catch (IOException e) {
              throw new RuntimeException(e);
            } finally {
              latch.countDown();
            }
          });
    }

    assertTrue(latch.await(10, TimeUnit.SECONDS));
    executor.shutdown();

    for (int i = 0; i < threads; i++) {
      Path p = tempDir.resolve("thread-dir-" + i);
      assertTrue(Files.isDirectory(p), "Directory " + i + " should exist");
      assertTrue(Files.isRegularFile(p.resolve(README_NAME)), "Readme for " + i + " should exist");
    }
  }

  @Test
  void create_Directories_parent_already_memorized() throws IOException {
    Path parent = tempDir.resolve("parent");
    Path child = parent.resolve("child");

    directoryDocumenter.memorize(parent, "# Parent");
    directoryDocumenter.createDirectories(parent);

    assertTrue(Files.isDirectory(parent));
    assertReadmeExists(parent, "# Parent");

    directoryDocumenter.memorize(child, "# Child");
    directoryDocumenter.createDirectories(child);

    assertTrue(Files.isDirectory(child));
    assertReadmeExists(child, "# Child");
    assertReadmeExists(parent, "# Parent");
  }

  @Test
  void readme_content_preserved_across_creates() throws IOException {
    Path dir = tempDir.resolve("persistent");
    String content = "# Important\nKeep this.";
    directoryDocumenter.memorize(dir, content);

    directoryDocumenter.createDirectories(dir);
    String firstRead = Files.readString(dir.resolve(README_NAME));

    directoryDocumenter.createDirectories(dir);
    String secondRead = Files.readString(dir.resolve(README_NAME));

    assertEquals(content, firstRead);
    assertEquals(firstRead, secondRead);
  }

  private void assertReadmeExists(Path dir, String expectedContent) throws IOException {
    Path readme = dir.resolve(README_NAME);
    assertTrue(Files.isRegularFile(readme), "Readme should exist in " + dir);
    assertEquals(expectedContent, Files.readString(readme));
  }
}
