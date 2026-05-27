package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.exception.CredentialIntegrityException;
import io.github.leawind.systemstoragelib.v1.impl.CredentialStoreImpl;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Persistently stores encrypted key-value pairs in a specific directory.
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
///
/// ### API Notes
///
/// - Keys are NOT stored in plaintext; only their digests or ciphertexts are stored.
/// - Integrity verification is enforced — tampered entries MUST be ignored.
/// - Keys are derived from the local environment.
public interface CredentialStore {
  static CredentialStore create(Storage storage) {
    return new CredentialStoreImpl(storage);
  }

  Storage storage();

  boolean exists(@NonNull String key);

  void set(@NonNull String key, @NonNull String value) throws IOException;

  /// Retrieves the value for the given key.
  ///
  /// @param key the key to look up
  /// @return the decrypted value, or `null` if the key does not exist
  /// @throws CredentialIntegrityException if the credential file is corrupted
  @Nullable String get(@NonNull String key) throws CredentialIntegrityException;

  /// Remove the entry for the given key if it exists. Do nothing if it does not exist.
  void remove(@NonNull String key) throws IOException;
}
