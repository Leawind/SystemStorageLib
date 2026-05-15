package io.github.leawind.systemstoragelib.v1.api.managers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.systemstoragelib.v1.impl.managers.StorageManagerImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageManagerTest {

  @TempDir Path tempDir;
  private static final Logger TEST_LOGGER = LoggerFactory.getLogger(StorageManagerTest.class);

  private StorageManager manager;

  @BeforeEach
  void setupEach() {
    manager = new StorageManagerImpl(TEST_LOGGER, tempDir.resolve("storage"));
  }

  @Test
  void testGetDirPathReturnsProvidedPath() {
    assertEquals(tempDir.resolve("storage"), manager.getDirPath());
  }

  @Test
  void testDirCreatedOnGetLock() {
    assertFalse(Files.exists(manager.getDirPath()));
  }

  @Test
  void testClearRemovesAllFilesExceptLock() throws IOException {
    try (var unused = LockUtils.writeLock(manager.getLock())) {
      Path dir = manager.getDirPath();
      Files.createDirectories(dir);
      Files.createFile(dir.resolve("file1.txt"));
      Files.createFile(dir.resolve("file2.txt"));
      Files.createDirectories(dir.resolve("subdir"));
      Files.createFile(dir.resolve("subdir/file3.txt"));

      manager.clear();

      assertFalse(Files.exists(dir.resolve("file1.txt")));
      assertFalse(Files.exists(dir.resolve("file2.txt")));
      assertFalse(Files.exists(dir.resolve("subdir/file3.txt")));
      assertFalse(Files.exists(dir.resolve("subdir")));

      // Directory itself should still exist
      assertTrue(Files.isDirectory(dir));
    }
  }

  @Test
  void testClearDoesNothingIfDirNotExists() throws IOException {
    // Don't create directory
    assertFalse(Files.exists(manager.getDirPath()));
    // Should not throw
    assertDoesNotThrow(() -> manager.clear());
  }

  @Test
  void testDeleteRemovesDirectoryAndLockFile() throws IOException {
    Path dir = manager.getDirPath();

    try (var unused = LockUtils.writeLock(manager.getLock())) {
      Files.createDirectories(dir);
      Files.createFile(dir.resolve("data.txt"));
    }

    manager.delete();

    assertFalse(Files.exists(dir));
  }

  @Nested
  class LockTest {

    @Test
    void testReadLockReentrant() throws IOException {
      manager.getLock().readLock().lock();
      manager.getLock().readLock().lock();
      manager.getLock().readLock().lock();

      manager.getLock().readLock().unlock();
      manager.getLock().readLock().unlock();
      manager.getLock().readLock().unlock();
    }

    @Test
    void testWriteLockReentrant() throws IOException {
      manager.getLock().writeLock().lock();
      manager.getLock().writeLock().lock();
      manager.getLock().writeLock().lock();

      manager.getLock().writeLock().unlock();
      manager.getLock().writeLock().unlock();
      manager.getLock().writeLock().unlock();
    }

    @Test
    void testReadLockCanBeAcquiredConcurrentlyByDifferentThreads() throws Exception {
      ExecutorService executor = Executors.newFixedThreadPool(2);
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(2);
      AtomicBoolean thread1Acquired = new AtomicBoolean(false);
      AtomicBoolean thread2Acquired = new AtomicBoolean(false);

      executor.submit(
          () -> {
            try {
              startLatch.await();
              manager.getLock().readLock().lock();
              thread1Acquired.set(true);
              // Hold lock briefly
              Thread.sleep(100);
              manager.getLock().readLock().unlock();
            } catch (Exception ignored) {
            } finally {
              doneLatch.countDown();
            }
          });

      executor.submit(
          () -> {
            try {
              startLatch.await();
              manager.getLock().readLock().lock();
              thread2Acquired.set(true);
              manager.getLock().readLock().unlock();
            } catch (Exception ignored) {
            } finally {
              doneLatch.countDown();
            }
          });

      startLatch.countDown();
      assertTrue(doneLatch.await(2, TimeUnit.SECONDS));

      executor.shutdownNow();

      // Both threads should have acquired the lock (concurrent read allowed)
      assertTrue(thread1Acquired.get());
      assertTrue(thread2Acquired.get());
    }

    @Test
    void testWriteLockDowngradeSupported() throws IOException {
      manager.getLock().writeLock().lock(); //   ═╗
      manager.getLock().readLock().lock(); //    ─║─┐
      manager.getLock().writeLock().unlock(); // ═╝ │
      manager.getLock().readLock().lock(); //    ─┐ │
      manager.getLock().readLock().unlock(); //  ─┘ │
      manager.getLock().readLock().unlock(); //  ───┘
    }

    @Test
    void testReadLockUpgradeThrowsException() throws IOException {
      manager.getLock().readLock().lock();
      // Trying to acquire write lock while holding read lock should throw
      assertThrows(Exception.class, manager.getLock().writeLock()::lock);
      manager.getLock().readLock().unlock();

      manager.getLock().writeLock().lock();
      manager.getLock().writeLock().unlock();
    }
  }
}
