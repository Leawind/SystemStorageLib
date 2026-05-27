package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.inventory.event.EventEmitter;
import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.inventory.misc.UncheckedCloseable;
import io.github.leawind.systemstoragelib.v1.api.Storage;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfigStore;
import io.github.leawind.systemstoragelib.v1.utils.AtomicFileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Stores configuration as JSON in `config.json` under the storage directory.
///
/// A background file watcher detects external modifications and re-reads the file,
/// emitting `onChanged` events when the parsed config differs from the cached value.
public class MetaConfigStoreImpl implements MetaConfigStore, AutoCloseable {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final String CONFIG_FILE_NAME = "config.json";
  private static final long FILE_CHANGE_DEBOUNCE_MS = 100;

  private final SystemStorageLib lib;
  private final Storage storage;
  private final Object watchStartLock = new Object();

  public final Codec<MetaConfig> CONFIG_CODEC;

  private final EventEmitter<MetaConfig> onChanged = new EventEmitter<>();

  private Path configFilePath;

  private volatile @Nullable MetaConfig config = null;
  private volatile @Nullable WatchService watchService = null;

  private long lastHandledFileChangeMs = 0;

  public MetaConfigStoreImpl(SystemStorageLib lib, Storage storage) {
    this.lib = lib;
    this.storage = storage;
    this.configFilePath =
        storage.getDirPath().resolve(CONFIG_FILE_NAME).toAbsolutePath().normalize();

    CONFIG_CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.unboundedMap(Codec.STRING, ScopeMetaConfigImpl.CODEC)
                            .fieldOf("scopes")
                            .forGetter(MetaConfig::scopes))
                    .apply(inst, (map) -> new MetaConfigImpl(lib, map)));

    storage.onDirUpdated().on(this, this::handleDirUpdated);

    ensureWatchStarted();
  }

  private void handleDirUpdated(Path oldDirPath) {
    Path dirPath = storage.getDirPath();
    if (dirPath.equals(oldDirPath)) {
      return;
    }

    synchronized (watchStartLock) {
      this.configFilePath = dirPath.resolve(CONFIG_FILE_NAME).toAbsolutePath().normalize();
      this.config = null;
      try {
        stopWatching();
      } catch (IOException e) {
        storage.logger().warn("Failed to stop watch service when updating dirPath", e);
      }
      ensureWatchStarted();
    }
  }

  @Override
  public Storage storage() {
    return storage;
  }

  @Override
  public MetaConfig get() throws IOException {
    MetaConfig cached = config;
    if (cached != null) {
      return cached;
    }

    synchronized (this) {
      if (config != null) {
        return config;
      }
      MetaConfig result = readAndCache();
      if (result != null) {
        return result;
      }

      config = createConfig();
      return config;
    }
  }

  @Override
  public void set(MetaConfig config) throws IOException {
    MetaConfig oldConfig = get();
    if (oldConfig.equals(config)) {
      return;
    }

    try (UncheckedCloseable ignored = LockUtils.lock(storage.getLock().writeLock())) {
      this.config = config;
      Files.createDirectories(storage.getDirPath());

      DataResult<JsonElement> result = CONFIG_CODEC.encodeStart(JsonOps.INSTANCE, config);
      Optional<JsonElement> encoded = result.result();
      if (encoded.isEmpty()) {
        storage.logger().warn("Failed to encode meta config: {}", result.error().orElse(null));
        return;
      }

      String json = GSON.toJson(encoded.get());
      AtomicFileWriter.write(configFilePath, json.getBytes(StandardCharsets.UTF_8));
    }

    synchronized (onChanged) {
      onChanged.emit(config);
    }
  }

  @Override
  public EventEmitter<MetaConfig> onChanged() {
    return onChanged;
  }

  @Override
  public MetaConfig createConfig() {
    return new MetaConfigImpl(lib, Collections.emptyMap());
  }

  @Override
  public void close() throws IOException {
    stopWatching();
  }

  public synchronized void stopWatching() throws IOException {
    WatchService ws = watchService;
    if (ws == null) {
      return;
    }

    try {
      ws.close();
    } catch (IOException e) {
      storage.logger().warn("Failed to close watch service", e);
      throw e;
    }

    watchService = null;
  }

  private @Nullable MetaConfig readAndCache() throws IOException {
    if (configFilePath == null) {
      return null;
    }

    if (!Files.exists(configFilePath)) {
      return null;
    }

    try (UncheckedCloseable ignored = LockUtils.lock(storage.getLock().readLock())) {
      String content = Files.readString(configFilePath, StandardCharsets.UTF_8);
      if (content.isEmpty()) {
        storage.logger().warn("Meta config file is empty: {}", configFilePath);
        return null;
      }

      JsonElement jsonElement;
      try {
        jsonElement = GSON.fromJson(content, JsonElement.class);
      } catch (JsonSyntaxException e) {
        storage
            .logger()
            .warn(
                "Failed to parse JSON in meta config file {}: {}", configFilePath, e.getMessage());
        return null;
      }

      DataResult<MetaConfig> parsedResult = CONFIG_CODEC.parse(JsonOps.INSTANCE, jsonElement);
      Optional<MetaConfig> parsed = parsedResult.result();
      if (parsed.isPresent()) {
        config = parsed.get();
        return config;
      } else {
        storage
            .logger()
            .warn(
                "Failed to parse meta config file {}: {}",
                configFilePath,
                parsedResult.error().orElse(null));
        return null;
      }
    }
  }

  private void ensureWatchStarted() {
    if (watchService != null) {
      return;
    }

    synchronized (watchStartLock) {
      if (watchService != null) {
        return;
      }

      try {
        WatchService ws = storage.getDirPath().getFileSystem().newWatchService();
        Files.createDirectories(storage.getDirPath());
        storage
            .getDirPath()
            .register(
                ws,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE);

        watchService = ws;
        Thread thread = new Thread(this::watchLoop, "meta-config-watcher");
        thread.setDaemon(true);
        thread.start();
      } catch (IOException e) {
        storage.logger().error("Failed to start file watcher for meta config", e);
      }
    }
  }

  private void watchLoop() {
    WatchService ws = watchService;
    if (ws == null) {
      storage.logger().error("Starting watch loop but watch service is null");
      return;
    }

    while (true) {
      WatchKey key;
      try {
        key = ws.take();
      } catch (InterruptedException e) {
        storage.logger().error("Watch loop interrupted", e);
        Thread.currentThread().interrupt();
        return;
      } catch (ClosedWatchServiceException e) {
        return;
      }

      boolean configFileChanged = false;
      for (WatchEvent<?> event : key.pollEvents()) {
        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
          storage.logger().info("Watch service overflow");
          continue;
        }
        if (storage
            .getDirPath()
            .resolve((Path) event.context())
            .toAbsolutePath()
            .normalize()
            .equals(configFilePath)) {
          configFileChanged = true;
        }
      }

      if (configFileChanged) {
        long now = System.currentTimeMillis();
        if (now - lastHandledFileChangeMs >= FILE_CHANGE_DEBOUNCE_MS) {
          handleFileChange();
          lastHandledFileChangeMs = now;
        }
      }

      if (!key.reset()) {
        break;
      }
    }
  }

  /// Handle file change events detected by the watch service.
  ///
  /// Re-reads the config file and emits `onChanged` if the parsed config differs from the cached
  /// value. If the config file was deleted, clears the cache so the next read returns the default.
  private void handleFileChange() {
    try {
      if (!Files.exists(configFilePath)) {
        config = null;
        return;
      }

      MetaConfig oldConfig = config;
      MetaConfig newConfig = readAndCache();

      if (newConfig != null && !Objects.equals(oldConfig, newConfig)) {
        synchronized (onChanged) {
          onChanged.emit(newConfig);
        }
      }
    } catch (IOException e) {
      storage.logger().error("Failed to handle meta config file change", e);
    }
  }
}
