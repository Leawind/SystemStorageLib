package io.github.leawind.systemstoragelib.v1.impl;

import dev.dirs.BaseDirectories;
import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.managers.CredentialStore;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import io.github.leawind.systemstoragelib.v1.impl.log.LogManager;
import io.github.leawind.systemstoragelib.v1.impl.log.SystemLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public class SystemStorageLibImpl implements SystemStorageLib {

  public static final String ROOT_DIR_NAME = "mc_system_storage";

  private final Path logsDir;
  private final Path credentialsDir;
  private final Path dataDir;
  private final Path configDir;
  private final Path cacheDir;
  private final Path dataLocalDir;

  private final LogManager logWriter;
  private final Map<String, ScopeStorageImpl> scopes = new ConcurrentHashMap<>();

  public SystemStorageLibImpl() {
    var baseDirs = BaseDirectories.get();
    logsDir = Path.of(baseDirs.dataDir, ROOT_DIR_NAME, "logs");
    credentialsDir = Path.of(baseDirs.dataDir, ROOT_DIR_NAME, "credentials");
    dataDir = Path.of(baseDirs.dataDir, ROOT_DIR_NAME, "data");
    configDir = Path.of(baseDirs.configDir, ROOT_DIR_NAME, "config");
    cacheDir = Path.of(baseDirs.cacheDir, ROOT_DIR_NAME, "cache");
    dataLocalDir = Path.of(baseDirs.dataLocalDir, ROOT_DIR_NAME, "dataLocal");

    logWriter = new LogManager(logsDir, 10 * 1024 * 1024, 10);

    detectScopes();
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
    return scopes.computeIfAbsent(scope, this::createScopeStorage);
  }

  @Override
  public Stream<String> getAllScopes() {
    return scopes.keySet().stream();
  }

  @Override
  public Path getLogsDir() {
    return logsDir;
  }

  private ScopeStorageImpl createScopeStorage(String scope) {
    return new ScopeStorageImpl(
        scope,
        new SystemLogger(logWriter, scope),
        CredentialStore.of(credentialsDir.resolve(scope)),
        StorageManager.of(dataDir.resolve(scope)),
        StorageManager.of(configDir.resolve(scope)),
        StorageManager.of(cacheDir.resolve(scope)),
        StorageManager.of(dataLocalDir.resolve(scope)));
  }

  private void detectScopes() {
    scanDirectoryForScopes(dataDir.resolve(ROOT_DIR_NAME));
    scanDirectoryForScopes(configDir.resolve(ROOT_DIR_NAME));
    scanDirectoryForScopes(cacheDir.resolve(ROOT_DIR_NAME));
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
          .forEach(scope -> scopes.putIfAbsent(scope, null));
    } catch (IOException e) {
      // Silently ignore scan failures
    }
  }
}
