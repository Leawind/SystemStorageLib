package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import io.github.leawind.systemstoragelib.v1.utils.MapUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public final class ScopeImpl implements Scope {

  private final String name;
  private final Logger logger;
  private final Map<StoreType, Path> dirs;
  private final DirectoryDocumenter directoryDocumenter;

  /// ### Throws {@link IllegalArgumentException}
  ///
  /// - If any StoreType is missing.
  /// - If any dirPath is not unique.
  ScopeImpl(
      String name,
      Logger logger,
      Map<StoreType, Path> dirs,
      DirectoryDocumenter directoryDocumenter) {

    List<StoreType> missingTypes = StoreType.Utils.missingTypes(dirs.keySet());
    if (!missingTypes.isEmpty()) {
      throw new IllegalArgumentException("Missing StoreTypes: " + missingTypes);
    }

    MapUtils.requireUniqueValues(dirs, "dirPath");

    this.name = name;
    this.logger = logger;
    this.dirs = dirs;
    this.directoryDocumenter = directoryDocumenter;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public Path directory(StoreType storeType) {
    Path path = dirs.get(storeType).resolve(name);
    try {
      directoryDocumenter.patrol(path);
    } catch (IOException e) {
      logger.warn("Failed to patrol directory: {}", path, e);
    }
    return path;
  }

  @Override
  public DirectoryDocumenter directoryDocumenter() {
    return directoryDocumenter;
  }
}
