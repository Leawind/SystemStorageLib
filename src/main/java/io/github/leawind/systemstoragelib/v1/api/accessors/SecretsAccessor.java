package io.github.leawind.systemstoragelib.v1.api.accessors;

import io.github.leawind.systemstoragelib.v1.Factory;
import io.github.leawind.systemstoragelib.v1.api.DirectoryAccessor;
import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import io.github.leawind.systemstoragelib.v1.api.exception.SecretIntegrityException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Persistently stores encrypted key-value pairs with expiration support in a specific directory.
/// Used for storing sensitive data such as API tokens, OAuth credentials, etc.
///
/// ### Goals
///
/// - Prevents accidental leakage via cloud sync, version control, or screenshots.
/// - Does NOT guarantee protection against malware running locally on the system.
/// - May tolerate data loss in certain situations, such as:
///   - User switching
///   - VM configuration changes
///   - OS reinstallation
///   - Other local environment changes
/// - Supports automatic expiration of secrets to enhance security
///
/// ### API Notes
///
/// - Keys are NOT stored in plaintext; only their digests or ciphertexts are stored.
/// - Keys can not be enumerated.
/// - Integrity verification is enforced.
/// - Keys are derived from the local environment.
/// - Expired secrets are treated as non-existent and automatically cleaned up on access.
public interface SecretsAccessor extends DirectoryAccessor {

  static SecretsAccessor from(Path dirPath, Scope scope, DirectoryDocumenter directoryDocumenter) {
    return Factory.createSecretsAccessor(dirPath, scope, directoryDocumenter);
  }

  // region Core methods

  /// Checks if the given key exists and has not expired.
  boolean exists(@NonNull String key);

  /// Set a key-value pair with an absolute expiration time, or no expiration.
  ///
  /// @param key        the key to store
  /// @param value      the value to encrypt and store
  /// @param expiresAt  the absolute time when the secret expires
  /// @throws IOException if the operation fails
  void set(@NonNull String key, @NonNull String value, @NonNull Instant expiresAt)
      throws IOException;

  /// Retrieves the value for the given key along with its metadata.
  /// Expired secrets are treated as non-existent.
  ///
  /// @param key the key to look up
  /// @return a `SecretEntry` if the key exists and hasn't expired, or `Optional.empty()` otherwise
  /// @throws SecretIntegrityException if the secret file is corrupted
  @NonNull Optional<SecretEntry> getEntry(@NonNull String key) throws SecretIntegrityException;

  /// Remove the entry for the given key if it exists. Do nothing if it does not exist.
  void remove(@NonNull String key) throws IOException;

  /// Returns the expiration time for the given key, if one is set.
  /// Note: This checks the raw stored expiration, even if it's in the past.
  ///
  /// @param key the key to query
  /// @return the expiration time, or `Optional.empty()` if no expiration is set or key doesn't
  // exist
  @NonNull Optional<Instant> getExpiration(@NonNull String key);

  /// Performs cleanup of expired secrets.
  ///
  /// @return the number of cleaned entries
  /// @throws IOException if the operation fails
  int cleanup() throws IOException;

  // endregion

  /// Set a key-value pair with no expiration.
  ///
  /// @implSpec The default implementation delegates to `set(key, value, null)`.
  default void set(@NonNull String key, @NonNull String value) throws IOException {
    set(key, value, Instant.MAX);
  }

  /// Set a key-value pair with an expiration duration relative to now.
  ///
  /// @implSpec The default implementation converts the duration to an absolute instant.
  default void set(@NonNull String key, @NonNull String value, @NonNull Duration ttl)
      throws IOException {
    set(key, value, Instant.now().plus(ttl));
  }

  /// Retrieves the value for the given key.
  /// Expired secrets are treated as non-existent and will return null.
  ///
  /// @implSpec The default implementation delegates to `getEntry`.
  default @Nullable String get(@NonNull String key) throws SecretIntegrityException {
    return getEntry(key).map(SecretEntry::value).orElse(null);
  }

  /// Updates the expiration time for an existing secret without modifying its value.
  ///
  /// @implSpec The default implementation reads the current value and re-sets it.
  default void updateExpiration(@NonNull String key, @NonNull Instant expiresAt)
      throws IOException, SecretIntegrityException {
    var entry =
        getEntry(key)
            .orElseThrow(
                () -> new IllegalStateException("Key does not exist or has expired: " + key));
    set(key, entry.value(), expiresAt);
  }

  /// Updates the expiration time relative to now for an existing secret.
  ///
  /// @implSpec The default implementation delegates to `updateExpiration(key, Instant)`.
  default void updateExpiration(@NonNull String key, @NonNull Duration ttl)
      throws IOException, SecretIntegrityException {
    updateExpiration(key, Instant.now().plus(ttl));
  }

  /// Removes expiration from a secret, making it permanent.
  ///
  /// @implSpec The default implementation delegates to `updateExpiration(key, null)`.
  default void makePermanent(@NonNull String key) throws IOException, SecretIntegrityException {
    updateExpiration(key, Instant.MAX);
  }

  /// A value object containing the decrypted secret and its optional expiration.
  record SecretEntry(@NonNull String value, @Nullable Instant expiresAt) {

    /// Returns `true` if the secret has already expired.
    public boolean isExpired() {
      return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
  }
}
