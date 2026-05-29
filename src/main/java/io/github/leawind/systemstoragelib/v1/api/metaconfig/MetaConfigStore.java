package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import io.github.leawind.inventory.event.EventEmitter;
import io.github.leawind.systemstoragelib.v1.api.StorageWrapper;
import java.io.IOException;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface MetaConfigStore extends StorageWrapper {
  /// Read the current configuration from disk, do not cache.
  ///
  /// If file not exist or corrupted, return a new default configuration but do not write to disk.
  MetaConfig get() throws IOException;

  /// Update the configuration. Write to disk if changed.
  ///
  /// Cross-process safe.
  ///
  /// ### Args
  ///
  /// - `updater`: The function to update the configuration.
  ///   The function accepts the current configuration and updates it in place.
  void update(Consumer<MetaConfig> updater) throws IOException;

  /// Fired when the configuration value changes.
  /// Either by {@link #update(Consumer)} or file changed by external process.
  ///
  /// Event: The new configuration.
  EventEmitter<ChangedEvent> onChanged();

  record ChangedEvent(@Nullable MetaConfig oldConfig, @NonNull MetaConfig config) {}
}
