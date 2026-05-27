package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MetaConfigStoreTest extends BaseTest {
  private MetaConfigStore store;
  private Path configFilePath;

  @TempDir Path customDir;

  @BeforeEach
  void setupEach() {
    store = lib.metaConfig();
    configFilePath = store.storage().getDirPath().resolve("config.json");
  }

  private MetaConfig getDefaultConfig() throws IOException {
    store.update(config -> config.scopes().clear());
    return store.get();
  }

  private MetaConfig setAsNonDefaultConfig() throws IOException {
    MetaConfig[] result = {null};
    store.update(
        config -> {
          config
              .scope("example_mod")
              .getCustomDirs()
              .put(StoreType.CONFIG, customDir.resolve("config"));
          result[0] = config;
        });
    return result[0];
  }

  private void waitForWatcherSettle() {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Nested
  class GetConfig {

    @Test
    void getReturnsConfigAfterSet() throws IOException {
      var config = setAsNonDefaultConfig();

      assertEquals(config, store.get());
    }

    @Test
    void getReturnsDefaultWhenConfigFileIsEmpty() throws IOException {
      Files.createDirectories(store.storage().getDirPath());
      Files.createFile(configFilePath);

      assertNotNull(store.get());
    }

    @Test
    void getReturnsDefaultWhenConfigFileIsMalformed() throws IOException {
      Files.createDirectories(store.storage().getDirPath());
      Files.writeString(configFilePath, "{invalid json content");

      assertNotNull(store.get());
    }
  }

  @Nested
  class UpdateConfig {

    @Test
    void updateDoesNotThrowWhenDirExists() throws IOException {
      Files.createDirectories(store.storage().getDirPath());
      assertDoesNotThrow(() -> store.update(config -> {}));
    }

    @Test
    void updateCreatesConfigFile() throws IOException {
      store.update(
          config ->
              config
                  .scope("example_mod")
                  .getCustomDirs()
                  .put(StoreType.DATA, customDir.resolve("data")));
      assertTrue(Files.exists(configFilePath));
    }

    @Test
    void updateIsIdempotentWithSameConfig() throws IOException {
      store.update(
          config ->
              config
                  .scope("example_mod")
                  .getCustomDirs()
                  .put(StoreType.CONFIG, customDir.resolve("config")));

      long modifiedTime1 = Files.getLastModifiedTime(configFilePath).toMillis();

      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      store.update(
          config ->
              config
                  .scope("example_mod")
                  .getCustomDirs()
                  .put(StoreType.CONFIG, customDir.resolve("config")));

      long modifiedTime2 = Files.getLastModifiedTime(configFilePath).toMillis();
      assertEquals(modifiedTime1, modifiedTime2);
    }
  }

  @Nested
  class WatchFileChanges {

    @Test
    void externalModificationTriggersOnChanged() throws IOException, InterruptedException {
      store.update(
          config ->
              config
                  .scope("scope1")
                  .getCustomDirs()
                  .put(StoreType.CONFIG, customDir.resolve("custom/config")));
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      store.onChanged().on(event -> latch.countDown());

      store.update(
          config ->
              config
                  .scope("scope2")
                  .getCustomDirs()
                  .put(StoreType.CONFIG, customDir.resolve("custom/config2")));

      assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
    }

    @Test
    void updateTriggersOnChanged() throws IOException, InterruptedException {
      store.update(
          config ->
              config
                  .scope("example_mod")
                  .getCustomDirs()
                  .put(StoreType.CONFIG, customDir.resolve("config")));
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      store.onChanged().on(event -> latch.countDown());

      store.update(config -> config.scopes().clear());

      assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void updateSameConfigDoesNotTriggerOnChanged() throws IOException, InterruptedException {
      store.update(
          config ->
              config
                  .scope("example_mod")
                  .getCustomDirs()
                  .put(StoreType.CONFIG, customDir.resolve("config")));
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      store.onChanged().on(event -> latch.countDown());

      store.update(
          config ->
              config
                  .scope("example_mod")
                  .getCustomDirs()
                  .put(StoreType.CONFIG, customDir.resolve("config")));

      assertFalse(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void malformedExternalFileTriggersOnChanged() throws IOException, InterruptedException {
      CountDownLatch latch = new CountDownLatch(1);
      store.onChanged().on(event -> latch.countDown());

      setAsNonDefaultConfig();

      waitForWatcherSettle();

      Files.writeString(configFilePath, "{invalid json content");

      assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }
  }
}
