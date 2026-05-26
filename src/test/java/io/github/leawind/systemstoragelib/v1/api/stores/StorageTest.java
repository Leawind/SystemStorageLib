package io.github.leawind.systemstoragelib.v1.api.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.inventory.misc.UncheckedCloseable;
import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.Storage;
import io.github.leawind.systemstoragelib.v1.impl.StorageImpl;
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

public class StorageTest extends BaseTest {

  private Storage manager;

  @BeforeEach
  void setupEach() {
    manager = new StorageImpl(lib, lib.logger(), tempDir.resolve("storage"));
  }

  @Test
  void testGetDirPathReturnsProvidedPath() {
    assertEquals(tempDir.resolve("storage"), manager.getDirPath());
  }

  @Test
  void testDirPathIsAbsoluteAndNormalized() {
    Path relativePath = tempDir.resolve("storage/../storage");
    Storage mgr = new StorageImpl(lib, lib.logger(), relativePath);
    assertEquals(relativePath.toAbsolutePath().normalize(), mgr.getDirPath());
  }

  @Test
  void testSetDirPathUpdatesPath() {
    Path newPath = tempDir.resolve("new-storage");
    manager.setDirPath(newPath);
    assertEquals(newPath, manager.getDirPath());
  }

  @Test
  void testSetDirPathResetsLock() throws IOException {
    // Acquire lock on original path
    manager.getLock().readLock().lock();
    manager.getLock().readLock().unlock();

    // Change dirPath — lock should be reset
    Path newPath = tempDir.resolve("new-storage");
    manager.setDirPath(newPath);

    // Lock should work on the new path
    assertDoesNotThrow(
        () -> {
          manager.getLock().writeLock().lock();
          manager.getLock().writeLock().unlock();
        });
  }

  @Test
  void testSetDirPathNormalizesPath() {
    Path newPath = tempDir.resolve("new-storage/../new-storage");
    manager.setDirPath(newPath);
    assertEquals(newPath.toAbsolutePath().normalize(), manager.getDirPath());
  }

  @Test
  void testSetDirPathDeletesOldLockFile() throws IOException {
    // Force lock file creation on original path
    manager.getLock().readLock().lock();
    manager.getLock().readLock().unlock();

    Path oldLockFile = manager.getDirPath().resolve(".lock");
    assertTrue(Files.exists(oldLockFile), "Lock file should exist after acquiring lock");

    Path newPath = tempDir.resolve("new-storage");
    manager.setDirPath(newPath);

    assertFalse(Files.exists(oldLockFile), "Old lock file should be deleted after setDirPath");
  }

  @Test
  void testSetDirPathCreatesLockUnderNewPath() throws IOException {
    Path newPath = tempDir.resolve("new-storage");
    manager.setDirPath(newPath);

    // Acquire lock — should create .lock file under new path
    manager.getLock().readLock().lock();
    manager.getLock().readLock().unlock();

    Path newLockFile = newPath.resolve(".lock");
    assertTrue(Files.exists(newLockFile), "Lock file should exist under new path");
  }

  @Test
  void testDirCreatedOnGetLock() {
    assertFalse(Files.exists(manager.getDirPath()));
  }

  @Test
  void testClearRemovesAllFilesExceptLock() throws IOException {
    try (UncheckedCloseable ignored = LockUtils.writeLock(manager.getLock())) {
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
  void testClearDoesNothingIfDirNotExists() {
    // Don't create directory
    assertFalse(Files.exists(manager.getDirPath()));
    // Should not throw
    assertDoesNotThrow(() -> manager.clear());
  }

  @Test
  void testDeleteRemovesDirectoryAndLockFile() throws IOException {
    Path dir = manager.getDirPath();

    try (UncheckedCloseable ignored = LockUtils.writeLock(manager.getLock())) {
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
      // ExecutorService is not AutoCloseable in java 17
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
