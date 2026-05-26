package io.github.leawind.systemstoragelib.v1.api.stores;

import io.github.leawind.inventory.event.EventEmitter;
import io.github.leawind.systemstoragelib.v1.api.Storage;
import io.github.leawind.systemstoragelib.v1.api.metaconfig.MetaConfig;
import java.io.IOException;

public interface MetaConfigManager {
  Storage storage();

  /// Get the current configuration.
  ///
  /// Returns the cached value if available; otherwise reads from disk.
  MetaConfig get() throws IOException;

  /// Update the configuration. Write to disk if changed.
  void set(MetaConfig config) throws IOException;

  /// Fired when the configuration value changes.
  /// Either by {@link #set(MetaConfig)} or file changed by external process.
  EventEmitter<MetaConfig> onChanged();

  /// Create a new default configuration.
  MetaConfig createConfig();
}
