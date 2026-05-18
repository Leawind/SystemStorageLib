package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.managers.MetaConfigManager;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface SystemStorageLib {

  Logger logger();

  MetaConfigManager metaConfig();

  /// ### Throws
  ///
  /// - `IllegalArgumentException` if scope is invalid.
  ScopeStorage scope(String scope);

  Stream<String> getAllScopes();

  /// - `2 <= length <= 63`
  /// - Must not start or end with `-`, `+`, `.`
  /// - Allowed characters:
  ///   - digits
  ///   - ASCII letters, lowercase and uppercase
  ///   - chars like `_`, `-`, `+`, `.`
  @Nullable String validateScope(String scope);

  default boolean isScopeValid(String scope) {
    return validateScope(scope) == null;
  }

  Path getLogsDir();

  static SystemStorageLib getInstance() {
    return SystemStorageLibHolder.INSTANCE;
  }
}
