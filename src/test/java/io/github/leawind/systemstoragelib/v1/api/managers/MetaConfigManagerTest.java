package io.github.leawind.systemstoragelib.v1.api.managers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.impl.SystemStorageLibImpl;
import io.github.leawind.systemstoragelib.v1.impl.managers.MetaConfigManagerImpl;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MetaConfigManagerTest {
  private static final Random RANDOM = new Random();
  private static final Gson GSON = new Gson();
  private static final FileSystem FS = Jimfs.newFileSystem(Configuration.unix());

  private final Path tempDir = FS.getPath("/tmp");

  private MetaConfigManager manager;

  private Map<StoreType<?>, Path> allDirs() {
    Map<StoreType<?>, Path> dirs = new HashMap<>();
    dirs.put(StoreType.CREDENTIALS, tempDir.resolve("credentials"));
    dirs.put(StoreType.CONFIG, tempDir.resolve("config"));
    dirs.put(StoreType.DATA, tempDir.resolve("data"));
    dirs.put(StoreType.CACHE, tempDir.resolve("cache"));
    dirs.put(StoreType.DATA_LOCAL, tempDir.resolve("data_local"));
    return dirs;
  }

  private MetaConfigManager createManager(Path metaConfigDir) {
    return new SystemStorageLibImpl(
            tempDir.resolve("logs"), metaConfigDir, allDirs(), 10 * 1024 * 1024, 10)
        .metaConfig();
  }

  private Path configFilePath() {
    return manager.getDirPath().resolve("config.json");
  }

  private MetaConfig createNonDefaultConfig() {
    MetaConfig config = MetaConfig.getDefault();
    config
        .getOrCreateScopeConfig("scope1")
        .setCustomDir(StoreType.CONFIG, tempDir.resolve("custom/config"));
    return config;
  }

  private MetaConfig createNonDefaultConfig2() {
    MetaConfig config = MetaConfig.getDefault();
    config
        .getOrCreateScopeConfig("scope2")
        .setCustomDir(StoreType.CONFIG, tempDir.resolve("custom/config2"));
    return config;
  }

  private void registerListener(Runnable onEvent) {
    synchronized (manager.onChanged()) {
      manager.onChanged().on(ignored -> onEvent.run());
    }
  }

  @BeforeEach
  void setupEach() throws IOException {
    Files.createDirectories(tempDir);
    manager = createManager(tempDir.resolve("meta" + RANDOM.nextInt()));
  }

  @AfterEach
  void tearDown() {
    try {
      ((MetaConfigManagerImpl) manager).stopWatching();
    } catch (IOException ignored) {
    }
  }

  @Nested
  class GetConfig {
    @Test
    void getReturnsDefaultWhenNoConfigFile() throws IOException {
      assertEquals(MetaConfig.getDefault(), manager.get());
    }

    @Test
    void getReturnsConfigAfterSet() throws IOException {
      manager.set(createNonDefaultConfig());

      MetaConfig config = MetaConfig.getDefault();
      manager.set(config);
      assertEquals(config, manager.get());
    }

    @Test
    void getReturnsConfigFromExistingValidFile() throws IOException {
      Files.createDirectories(manager.getDirPath());
      Files.writeString(configFilePath(), "{\"custom_dirs\":{}}");

      MetaConfig result = manager.get();
      assertNotNull(result);
    }

    @Test
    void getReturnsDefaultWhenConfigFileIsEmpty() throws IOException {
      Files.createDirectories(manager.getDirPath());
      Files.createFile(configFilePath());
      assertNotNull(manager.get());
    }

    @Test
    void getReturnsDefaultWhenConfigFileIsMalformed() throws IOException {
      Files.createDirectories(manager.getDirPath());
      Files.writeString(configFilePath(), "{invalid json content");
      assertNotNull(manager.get());
    }
  }

  @Nested
  class SetConfig {
    @Test
    void setDoesNotThrowWhenDirExists() throws IOException {
      Files.createDirectories(manager.getDirPath());
      assertDoesNotThrow(() -> manager.set(MetaConfig.getDefault()));
    }

    @Test
    void setCreatesConfigFile() throws IOException {
      manager.set(MetaConfig.getDefault());
      assertFalse(Files.exists(configFilePath()));
      manager.set(createNonDefaultConfig());
      assertTrue(Files.exists(configFilePath()));
    }

    @Test
    void setIsIdempotentWithSameReference() throws IOException {
      Path configPath = configFilePath();

      manager.set(createNonDefaultConfig());
      long modifiedTime1 = Files.getLastModifiedTime(configPath).toMillis();

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      manager.set(createNonDefaultConfig());
      long modifiedTime2 = Files.getLastModifiedTime(configPath).toMillis();

      assertEquals(modifiedTime1, modifiedTime2, "File should not be modified on idempotent set");
    }
  }

  @Nested
  class WatchFileChanges {

    /// Sleep enough time for the watcher thread to process any pending file events.
    private void waitForWatcherSettle() {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    private String toJson(MetaConfig config) {
      JsonElement element =
          MetaConfig.CODEC.encodeStart(JsonOps.INSTANCE, config).result().orElseThrow();
      return GSON.toJson(element);
    }

    @Test
    void externalModificationTriggersOnChanged() throws IOException, InterruptedException {
      manager.set(createNonDefaultConfig());
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      registerListener(latch::countDown);

      MetaConfig differentConfig = createNonDefaultConfig2();
      Files.writeString(configFilePath(), toJson(differentConfig));

      assertTrue(
          latch.await(8000, TimeUnit.MILLISECONDS),
          "onChanged should be triggered when config file is modified externally");
    }

    @Test
    void setTriggersOnChanged() throws IOException, InterruptedException {
      manager.set(createNonDefaultConfig());
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      registerListener(latch::countDown);

      manager.set(MetaConfig.getDefault());

      assertTrue(
          latch.await(500, TimeUnit.MILLISECONDS),
          "onChanged should be triggered when set() writes a different config");
    }

    @Test
    void setSameConfigDoesNotTriggerOnChanged() throws IOException, InterruptedException {
      manager.set(createNonDefaultConfig());
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      registerListener(latch::countDown);

      manager.set(createNonDefaultConfig());

      assertFalse(
          latch.await(500, TimeUnit.MILLISECONDS),
          "onChanged should NOT be triggered when set() is called with the same config");
    }

    @Test
    void deleteRemovesDirectory() throws IOException {
      manager.set(createNonDefaultConfig());
      assertTrue(Files.isDirectory(manager.getDirPath()));

      manager.delete();

      assertFalse(Files.exists(manager.getDirPath()));
    }

    @Test
    void malformedExternalFileDoesNotTriggerOnChanged() throws IOException, InterruptedException {
      manager.set(createNonDefaultConfig());
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      registerListener(latch::countDown);

      Files.writeString(configFilePath(), "{invalid json content");

      assertFalse(
          latch.await(500, TimeUnit.MILLISECONDS),
          "onChanged should NOT be triggered when external file contains invalid JSON");
    }
  }
}
