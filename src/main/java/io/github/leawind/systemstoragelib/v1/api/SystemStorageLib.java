package io.github.leawind.systemstoragelib.v1.api;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface SystemStorageLib {

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

  /// ### Throws
  ///
  /// - `IllegalArgumentException` if scope is invalid.
  ScopeStorage getScopeStorage(String scope);

  Stream<String> scopes();

  Path getLogDir();

  static SystemStorageLib getInstance() {
    return SystemStorageLibHolder.INSTANCE;
  }
}
