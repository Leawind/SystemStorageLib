package io.github.leawind.systemstoragelib.v1.api.accessors;

import io.github.leawind.systemstoragelib.v1.api.DirectoryAccessor;
import io.github.leawind.systemstoragelib.v1.api.exception.SecretIntegrityException;
import io.github.leawind.systemstoragelib.v1.impl.accessors.SecretsAccessorImpl;
import java.io.IOException;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface SecretsAccessor extends DirectoryAccessor {
  static SecretsAccessor from(Path dirPath, Logger logger) {
    return new SecretsAccessorImpl(dirPath, logger);
  }

  boolean exists(@NonNull String key);

  void set(@NonNull String key, @NonNull String value) throws IOException;

  /// Retrieves the value for the given key.
  ///
  /// @param key the key to look up
  /// @return the decrypted value, or `null` if the key does not exist
  /// @throws SecretIntegrityException if the secret file is corrupted
  @Nullable String get(@NonNull String key) throws SecretIntegrityException;

  /// Remove the entry for the given key if it exists. Do nothing if it does not exist.
  void remove(@NonNull String key) throws IOException;
}
