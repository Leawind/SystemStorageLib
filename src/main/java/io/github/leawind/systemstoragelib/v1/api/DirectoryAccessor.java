package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.inventory.event.EventEmitter;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

public interface DirectoryAccessor {
  Path getDirPath();

  void setDirPath(Path dirPath);

  Logger getLogger();

  void setLogger(@NonNull Logger logger);

  EventEmitter<Path> onDirPathChanged();
}
