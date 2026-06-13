package io.github.leawind.systemstoragelib.v1.api.accessors;

import io.github.leawind.inventory.event.SimpleEventEmitter;
import io.github.leawind.systemstoragelib.v1.api.DirectoryAccessor;
import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

public class AbstractDirectoryAccessor implements DirectoryAccessor {
  private Path dirPath;
  private Logger logger;
  protected final DirectoryDocumenter directoryDocumenter;
  protected final SimpleEventEmitter.Owned<Path> onDirPathChanged = SimpleEventEmitter.create();

  protected AbstractDirectoryAccessor(
      Path dirPath, Logger logger, DirectoryDocumenter directoryDocumenter) {
    this.dirPath = dirPath.normalize();
    this.logger = logger;
    this.directoryDocumenter = directoryDocumenter;
  }

  @Override
  public Path getDirPath() {
    return dirPath;
  }

  @Override
  public void setDirPath(Path newPath) {
    newPath = newPath.normalize();
    if (this.dirPath.equals(newPath)) {
      return;
    }
    this.dirPath = newPath;
    onDirPathChanged.emit(newPath);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public void setLogger(@NonNull Logger logger) {
    this.logger = logger;
  }

  @Override
  public SimpleEventEmitter<Path> onDirPathChanged() {
    return onDirPathChanged;
  }
}
