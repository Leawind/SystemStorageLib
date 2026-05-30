package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.inventory.event.EventEmitter;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

public interface DirectoryAccessor {
  /// Returns the storage directory path, always normalized.
  Path getDirPath();

  /// Updates the storage directory path.
  ///
  /// The path will be normalized
  void setDirPath(Path dirPath);

  Logger getLogger();

  void setLogger(@NonNull Logger logger);

  /// Emits when the storage directory path is changed.
  EventEmitter<Path> onDirPathChanged();
}
