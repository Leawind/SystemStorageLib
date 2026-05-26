package io.github.leawind.systemstoragelib.v1.api.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.ScopeMetaConfig;
import io.github.leawind.systemstoragelib.v1.impl.stores.MetaConfigStoreImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MetaConfigStoreTest extends BaseTest {
  private static final Gson GSON = new Gson();

  private MetaConfigStoreImpl store;

  @BeforeEach
  void setupEach() {
    store = (MetaConfigStoreImpl) lib.metaConfig();
  }

  @AfterEach
  void tearDown() {
    try {
      store.stopWatching();
    } catch (IOException ignored) {
    }
  }

  private Path configFilePath() {
    return store.storage().getDirPath().resolve("config.json");
  }

  private MetaConfig createNonDefaultConfig() {
    MetaConfig config = store.createConfig();
    ScopeMetaConfig scopeMetaConfig =
        config.scopes().computeIfAbsent("scope1", ignored -> config.createScopeConfig());
    scopeMetaConfig.getCustomDirs().put(StoreType.CONFIG, tempDir.resolve("custom/config"));
    return config;
  }

  private MetaConfig createNonDefaultConfig2() {
    MetaConfig config = store.createConfig();
    ScopeMetaConfig scopeMetaConfig =
        config.scopes().computeIfAbsent("scope2", ignored -> config.createScopeConfig());
    scopeMetaConfig.getCustomDirs().put(StoreType.CONFIG, tempDir.resolve("custom/config2"));
    return config;
  }

  private void registerListener(Runnable onEvent) {
    synchronized (store.onChanged()) {
      store.onChanged().on(ignored -> onEvent.run());
    }
  }

  @Nested
  class GetConfig {
    @Test
    void getReturnsDefaultWhenNoConfigFile() throws IOException {
      assertEquals(store.createConfig(), store.get());
    }

    @Test
    void getReturnsConfigAfterSet() throws IOException {
      store.set(createNonDefaultConfig());

      MetaConfig config = store.createConfig();
      store.set(config);
      assertEquals(config, store.get());
    }

    @Test
    void getReturnsConfigFromExistingValidFile() throws IOException {
      Files.createDirectories(store.storage().getDirPath());
      Files.writeString(configFilePath(), "{\"custom_dirs\":{}}");

      MetaConfig result = store.get();
      assertNotNull(result);
    }

    @Test
    void getReturnsDefaultWhenConfigFileIsEmpty() throws IOException {
      Files.createDirectories(store.storage().getDirPath());
      Files.createFile(configFilePath());
      assertNotNull(store.get());
    }

    @Test
    void getReturnsDefaultWhenConfigFileIsMalformed() throws IOException {
      Files.createDirectories(store.storage().getDirPath());
      Files.writeString(configFilePath(), "{invalid json content");
      assertNotNull(store.get());
    }
  }

  @Nested
  class SetConfig {
    @Test
    void setDoesNotThrowWhenDirExists() throws IOException {
      Files.createDirectories(store.storage().getDirPath());
      assertDoesNotThrow(() -> store.set(store.createConfig()));
    }

    @Test
    void setCreatesConfigFile() throws IOException {
      store.set(store.createConfig());
      assertFalse(Files.exists(configFilePath()));
      store.set(createNonDefaultConfig());
      assertTrue(Files.exists(configFilePath()));
    }

    @Test
    void setIsIdempotentWithSameReference() throws IOException {
      Path configPath = configFilePath();

      store.set(createNonDefaultConfig());
      long modifiedTime1 = Files.getLastModifiedTime(configPath).toMillis();

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      store.set(createNonDefaultConfig());
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
          store.CONFIG_CODEC.encodeStart(JsonOps.INSTANCE, config).result().orElseThrow();
      return GSON.toJson(element);
    }

    @Test
    void externalModificationTriggersOnChanged() throws IOException, InterruptedException {
      store.set(createNonDefaultConfig());
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
      store.set(createNonDefaultConfig());
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      registerListener(latch::countDown);

      store.set(store.createConfig());

      assertTrue(
          latch.await(500, TimeUnit.MILLISECONDS),
          "onChanged should be triggered when set() writes a different config");
    }

    @Test
    void setSameConfigDoesNotTriggerOnChanged() throws IOException, InterruptedException {
      store.set(createNonDefaultConfig());
      waitForWatcherSettle();

      CountDownLatch latch = new CountDownLatch(1);
      registerListener(latch::countDown);

      store.set(createNonDefaultConfig());

      assertFalse(
          latch.await(500, TimeUnit.MILLISECONDS),
          "onChanged should NOT be triggered when set() is called with the same config");
    }

    @Test
    void malformedExternalFileDoesNotTriggerOnChanged() throws IOException, InterruptedException {
      store.set(createNonDefaultConfig());
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
