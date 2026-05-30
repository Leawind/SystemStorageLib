package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.leawind.inventory.event.EventEmitter;
import io.github.leawind.inventory.lock.FileBasedReentrantReadWriteLock;
import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.inventory.misc.Lazy;
import io.github.leawind.inventory.misc.UncheckedCloseable;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.accessors.AbstractDirectoryAccessor;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfigStore;
import io.github.leawind.systemstoragelib.v1.utils.AtomicFileWriter;
import io.github.leawind.systemstoragelib.v1.utils.Codecs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/// Stores configuration as JSON in `config.json` under the storage directory.
///
/// A background file watcher detects external modifications and re-reads the file,
/// emitting `onChanged` events when the parsed config differs from the cached value.
public class MetaConfigStoreImpl extends AbstractDirectoryAccessor
    implements MetaConfigStore, AutoCloseable {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final String CONFIG_FILE_NAME = "config.json";
  private static final long FILE_CHANGE_DEBOUNCE_MS = 100;

  private final SystemStorageLib lib;
  private final Codec<MetaConfig> configCodec;

  private Path configFilePath;
  private Lazy<FileBasedReentrantReadWriteLock> lock;

  private final Object watchStartLock = new Object();

  private final EventEmitter<ChangedEvent> onChanged = new EventEmitter<>();

  private volatile @Nullable MetaConfig cache = null;
  private volatile @Nullable WatchService watchService = null;

  private long lastHandledFileChangeMs = 0;

  public MetaConfigStoreImpl(SystemStorageLib lib, Path dirPath, Logger logger) {
    super(dirPath, logger);
    this.lib = lib;
    configCodec = MetaConfigImpl.codec(lib);

    onDirPathChanged().on(this, this::handleDirUpdated);
    onDirPathChanged().emit(null);
  }

  @Override
  public MetaConfig get() throws IOException {
    if (!Files.exists(resolveConfigFilePath())) {
      return new MetaConfigImpl(lib);
    }
    try (UncheckedCloseable ignored = LockUtils.readLock(lock.get())) {
      return readOrDefaultUnsafe();
    }
  }

  @Override
  public void update(Consumer<MetaConfig> updater) throws IOException {
    try (UncheckedCloseable ignored = LockUtils.writeLock(lock.get())) {
      MetaConfig oldConfig = readOrDefaultUnsafe();
      MetaConfig newConfig = Codecs.clone(oldConfig, configCodec, JsonOps.INSTANCE);

      updater.accept(newConfig);

      if (oldConfig.equals(newConfig)) {
        return;
      }

      cache = newConfig;

      Files.createDirectories(configFilePath.getParent());

      DataResult<JsonElement> result = configCodec.encodeStart(JsonOps.INSTANCE, newConfig);
      Optional<JsonElement> encoded = result.result();
      if (encoded.isEmpty()) {
        getLogger().error("Failed to encode meta config: {}", result.error().orElse(null));
        return;
      }

      String json = GSON.toJson(encoded.get());
      AtomicFileWriter.write(configFilePath, json.getBytes(StandardCharsets.UTF_8));

      synchronized (onChanged) {
        onChanged.emit(new ChangedEvent(oldConfig, newConfig));
      }
    }
  }

  @Override
  public EventEmitter<ChangedEvent> onChanged() {
    return onChanged;
  }

  @Override
  public void close() throws IOException {
    stopWatching();
  }

  /// ### Returns
  ///
  /// - The current configuration from disk if file exist and valid.
  /// - `null` if:
  ///   - file not exist.
  ///   - file is empty.
  ///   - file content is not valid JSON.
  ///   - file content json is not valid meta config.
  ///
  /// ### Throws
  ///
  /// - `IOException` if file read failed.
  private @Nullable MetaConfig readUnsafe() throws IOException {
    if (!Files.exists(configFilePath)) {
      return null;
    }

    String content = Files.readString(configFilePath, StandardCharsets.UTF_8);
    if (content.isEmpty()) {
      getLogger().warn("Meta config file is empty: {}", configFilePath);
      return null;
    }

    JsonElement jsonElement;
    try {
      jsonElement = GSON.fromJson(content, JsonElement.class);
    } catch (JsonSyntaxException e) {
      getLogger()
          .warn("Failed to parse JSON in meta config file {}: {}", configFilePath, e.getMessage());
      return null;
    }

    DataResult<MetaConfig> parsedResult = configCodec.parse(JsonOps.INSTANCE, jsonElement);
    Optional<MetaConfig> parsed = parsedResult.result();

    if (parsed.isEmpty()) {
      getLogger()
          .warn(
              "Failed to parse meta config file {}: {}",
              configFilePath,
              parsedResult.error().orElse(null));
      return null;
    }

    cache = parsed.get();
    return cache;
  }

  /// ### Returns
  ///
  /// - The current configuration from disk if file exist and valid.
  /// - A new default configuration if:
  ///   - file not exist.
  ///   - file is empty.
  ///
  /// ### Throws
  ///
  /// - `IOException` if file read failed.
  private MetaConfig readOrDefaultUnsafe() throws IOException {
    MetaConfig result = readUnsafe();
    if (result == null) {
      return new MetaConfigImpl(lib);
    }
    return result;
  }

  /// Ensure the file watcher is started.
  ///
  /// Create if the directory not exist
  private void ensureWatchStarted() {
    if (watchService != null) {
      return;
    }

    synchronized (watchStartLock) {
      if (watchService != null) {
        return;
      }

      try {
        WatchService ws = getDirPath().getFileSystem().newWatchService();
        Files.createDirectories(getDirPath());
        getDirPath()
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
        getLogger().error("Failed to start file watcher for meta config", e);
      }
    }
  }

  private void watchLoop() {
    WatchService ws = watchService;
    if (ws == null) {
      getLogger().error("Starting watch loop but watch service is null");
      return;
    }

    while (true) {
      WatchKey key;
      try {
        key = ws.take();
      } catch (InterruptedException e) {
        getLogger().error("Watch loop interrupted", e);
        Thread.currentThread().interrupt();
        return;
      } catch (ClosedWatchServiceException e) {
        return;
      }

      boolean configFileChanged = false;
      for (WatchEvent<?> event : key.pollEvents()) {
        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
          getLogger().info("Watch service overflow");
          continue;
        }
        if (getDirPath()
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

  private synchronized void stopWatching() throws IOException {
    WatchService ws = watchService;
    if (ws == null) {
      return;
    }

    try {
      ws.close();
    } catch (IOException e) {
      getLogger().warn("Failed to close watch service", e);
      throw e;
    }

    watchService = null;
  }

  /// Handle meta config file change events detected by the watch service.
  ///
  /// Re-reads the config file and emits `onChanged` if the parsed config differs from the cached
  /// value. If the config file was deleted, clears the cache so the next read returns the default.
  private synchronized void handleFileChange() {
    synchronized (onChanged) {
      if (cache == null) {
        cache = new MetaConfigImpl(lib);
      }

      MetaConfig newConfig;

      try {
        newConfig = readOrDefaultUnsafe();
      } catch (IOException e) {
        getLogger()
            .warn("IOException occurred while reading meta config file handling file change", e);
        return;
      }

      if (!Objects.equals(cache, newConfig)) {
        onChanged.emit(new ChangedEvent(cache, newConfig));
        cache = newConfig;
      }
    }
  }

  private void handleDirUpdated(@Nullable Path oldDirPath) {
    if (getDirPath().equals(oldDirPath)) {
      return;
    }

    synchronized (watchStartLock) {
      this.configFilePath = resolveConfigFilePath();
      this.lock = new Lazy<>(() -> createLock(configFilePath));
      this.cache = null;
      try {
        stopWatching();
      } catch (IOException e) {
        getLogger().warn("Failed to stop watch service when updating dirPath", e);
      }
      ensureWatchStarted();
    }

    handleFileChange();
  }

  private Path resolveConfigFilePath() {
    return getDirPath().resolve(CONFIG_FILE_NAME).normalize();
  }
}
