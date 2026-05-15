package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.impl.log.LogManager;
import io.github.leawind.systemstoragelib.v1.impl.log.SystemLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public class SystemStorageLibImpl implements SystemStorageLib {

  public static final String ROOT_DIR_NAME = "mc_system_storage";

  private final Path logsDir;

  /// ### Examples
  ///
  /// - config: `/home/Steve/.config/mc_system_storage/config`.
  /// - data: `/home/Steve/.local/share/mc_system_storage/data`.
  private final Map<StoreType<?>, Path> scopedDirs;

  private final LogManager logWriter;
  private final Map<String, Optional<ScopeStorage>> scopes = new ConcurrentHashMap<>();

  /// ### Args
  ///
  /// #### `logsDir`
  ///
  /// Directory to store logs.
  ///
  /// #### `scopedDirs`
  ///
  /// Map of {@link StoreType} to {@link Path} of directory.
  ///
  /// - Must contain all {@link StoreType}.
  /// - Must be unique for each {@link StoreType}.
  ///
  /// Example:
  ///
  /// - config: `/home/Steve/.config/mc_system_storage/config`.
  /// - data: `/home/Steve/.local/share/mc_system_storage/data`.
  ///
  /// ### Throws {@link IllegalArgumentException}
  ///
  /// - If `scopedDirs` does not contain all {@link StoreType}.
  /// - If any value in `scopedDirs` is not unique.
  public SystemStorageLibImpl(Path logsDir, Map<StoreType<?>, Path> scopedDirs) {
    validateDirs(scopedDirs);

    this.logsDir = logsDir;
    this.scopedDirs = Map.copyOf(scopedDirs);

    logWriter = new LogManager(logsDir, 10 * 1024 * 1024, 10);
    detectScopes();
  }

  private static void validateDirs(Map<StoreType<?>, Path> scopedDirs)
      throws IllegalArgumentException {
    // Check for missing StoreTypes.
    List<StoreType<?>> missingTypes = StoreType.Utils.missingTypes(scopedDirs.keySet());
    if (!missingTypes.isEmpty()) {
      throw new IllegalArgumentException("Missing StoreTypes: " + missingTypes);
    }

    // Check for unique scopedDirs for each StoreType.
    for (var entry : scopedDirs.entrySet()) {
      for (var other : scopedDirs.entrySet()) {
        if (!entry.getKey().equals(other.getKey()) && entry.getValue().equals(other.getValue())) {
          throw new IllegalArgumentException(
              "dir for each StoreType must be unique, but "
                  + entry.getKey()
                  + " and "
                  + other.getKey()
                  + " are the same: "
                  + entry.getValue());
        }
      }
    }
  }

  @Override
  public @Nullable String validateScope(String scope) {
    if (scope.isEmpty()) {
      return "scope is empty";
    }

    if (!(2 <= scope.length() && scope.length() <= 63)) {
      return "scope length must be between 2 and 63 characters";
    }

    char firstChar = scope.charAt(0);
    switch (firstChar) {
      case '-', '+', '.':
        return "scope must not start with `" + firstChar + "`";
    }

    char lastChar = scope.charAt(scope.length() - 1);
    switch (lastChar) {
      case '-', '+', '.':
        return "scope must not end with `" + lastChar + "`";
    }

    for (char c : scope.toCharArray()) {
      if (c >= 'A' && c <= 'Z') continue;
      if (c >= 'a' && c <= 'z') continue;
      if (c >= '0' && c <= '9') continue;
      switch (c) {
        case '_', '-', '+', '.':
          continue;
        default:
          return "scope contains invalid characters: " + c;
      }
    }

    return null;
  }

  @Override
  public ScopeStorage scope(String scope) {
    validateScope(scope);

    if (scopes.containsKey(scope)) {
      var optional = scopes.get(scope);
      if (optional.isPresent()) {
        return optional.get();
      }
    }

    var storage = createScopeStorage(scope);
    scopes.put(scope, Optional.of(storage));
    return storage;
  }

  @Override
  public Stream<String> getAllScopes() {
    detectScopes();
    return scopes.keySet().stream();
  }

  @Override
  public Path getLogsDir() {
    return logsDir;
  }

  private ScopeStorage createScopeStorage(String scope) {
    return ScopeStorage.ofDirs(scope, new SystemLogger(logWriter, scope), scopedDirs);
  }

  private void detectScopes() {
    scopes.values().removeIf(Optional::isEmpty);
    scopedDirs.values().forEach(this::scanDirectoryForScopes);
  }

  private void scanDirectoryForScopes(Path rootDir) {
    if (!Files.isDirectory(rootDir)) {
      return;
    }
    try (var entries = Files.list(rootDir)) {
      entries
          .filter(Files::isDirectory)
          .map(p -> p.getFileName().toString())
          .filter(this::isScopeValid)
          .forEach(scope -> scopes.putIfAbsent(scope, Optional.empty()));
    } catch (IOException ignored) {
    }
  }
}
