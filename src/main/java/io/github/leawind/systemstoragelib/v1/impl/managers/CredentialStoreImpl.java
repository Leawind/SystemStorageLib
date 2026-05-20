package io.github.leawind.systemstoragelib.v1.impl.managers;

import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.inventory.misc.UncheckedCloseable;
import io.github.leawind.systemstoragelib.v1.api.exception.CredentialIntegrityException;
import io.github.leawind.systemstoragelib.v1.api.managers.CredentialStore;
import io.github.leawind.systemstoragelib.v1.utils.AtomicFileWriter;
import io.github.leawind.systemstoragelib.v1.utils.machineid.MachineIdProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/// AES-256-GCM encrypted credential storage with environment-bound key derivation.
///
/// File names are `{sha256_hex}.enc` (e.g. `a1b2c3d4...e5f6.enc`);
/// Binary format per file:
///
/// | Offset | Size | Field |
/// |--|--|--|
/// | `0x00` | 1B | Version (`0x01`) |
/// | `0x01` | 12B | IV / Nonce |
/// | `0x0D` | NB | Ciphertext |
/// | EOF-16 | 16B | GCM Auth Tag |
///
/// AES key derived via PBKDF2 from `user.name:user.home`.
public class CredentialStoreImpl extends StorageManagerImpl implements CredentialStore {

  private static final byte FORMAT_VERSION = 0x01;
  private static final int IV_LENGTH = 12;
  private static final int AUTH_TAG_LENGTH = 16;
  private static final int MIN_FILE_SIZE = 1 + IV_LENGTH + AUTH_TAG_LENGTH; // 29 bytes

  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int AES_KEY_LENGTH_BITS = 256;

  private static final String FILE_SUFFIX = ".enc";

  private static final Set<PosixFilePermission> DIR_PERMISSIONS =
      PosixFilePermissions.fromString("rwx------");
  private static final Set<PosixFilePermission> FILE_PERMISSIONS =
      PosixFilePermissions.fromString("rw-------");
  private static final FileAttribute<Set<PosixFilePermission>> DIR_ATTRIBUTE =
      PosixFilePermissions.asFileAttribute(DIR_PERMISSIONS);

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private volatile SecretKey aesKey;

  public CredentialStoreImpl(Logger logger, Path dirPath) {
    super(logger, dirPath);
  }

  @Override
  public boolean exists(@NonNull String key) {
    return Files.exists(keyToFilePath(key));
  }

  @Override
  public void set(@NonNull String key, @NonNull String value) throws IOException {
    validateKey(key);
    Path filePath = keyToFilePath(key);

    try (UncheckedCloseable ignored = LockUtils.lock(getLock().writeLock())) {
      ensureDirectoryExists();

      byte[] plaintext = value.getBytes(StandardCharsets.UTF_8);
      byte[] encrypted = encrypt(plaintext);

      AtomicFileWriter.write(filePath, encrypted);
      applyFilePermissions(filePath);
    } catch (InvalidAlgorithmParameterException
        | BadPaddingException
        | IllegalBlockSizeException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      throw new RuntimeException(e);
    }
  }

  /// @throws CredentialIntegrityException if the file is corrupted
  @Override
  public @Nullable String get(@NonNull String key)
      throws CredentialIntegrityException, IOException {
    validateKey(key);
    Path filePath = keyToFilePath(key);
    if (!Files.exists(filePath)) {
      return null;
    }

    try (UncheckedCloseable ignored = LockUtils.lock(getLock().readLock())) {
      byte[] fileContent = Files.readAllBytes(filePath);
      validateFileSize(fileContent, filePath);
      byte[] plaintext = decryptFileContent(fileContent, filePath);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (InvalidAlgorithmParameterException
        | NoSuchPaddingException
        | IllegalBlockSizeException
        | NoSuchAlgorithmException
        | BadPaddingException
        | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  /// Deletion failure is logged as a warning and does not propagate.
  @Override
  public void remove(@NonNull String key) throws IOException {
    Files.deleteIfExists(keyToFilePath(key));
  }

  // region Key validation and hashing

  private void validateKey(String key) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Key must not be null or empty");
    }
  }

  private Path keyToFilePath(String key) {
    return getDirPath().resolve(sha256Hex(key) + FILE_SUFFIX);
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  // endregion

  // region Encryption and decryption

  private SecretKey getOrCreateKey() {
    if (aesKey == null) {
      synchronized (this) {
        if (aesKey == null) {
          aesKey = deriveKey();
        }
      }
    }
    return aesKey;
  }

  private SecretKey deriveKey() {
    try {
      String keyMaterial =
          System.getProperty("user.name")
              + ":"
              + System.getProperty("user.home")
              + ":"
              + MachineIdProvider.getMachineId();
      byte[] salt = "SystemStorageLib-CredentialStore-v1".getBytes(StandardCharsets.UTF_8);

      PBEKeySpec keySpec =
          new PBEKeySpec(keyMaterial.toCharArray(), salt, 65536, AES_KEY_LENGTH_BITS);
      SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
      keySpec.clearPassword();

      return new SecretKeySpec(keyBytes, "AES");
    } catch (Exception e) {
      throw new IllegalStateException("Failed to derive encryption key", e);
    }
  }

  private byte[] encrypt(byte[] plaintext)
      throws NoSuchPaddingException,
          NoSuchAlgorithmException,
          InvalidAlgorithmParameterException,
          InvalidKeyException,
          IllegalBlockSizeException,
          BadPaddingException {
    byte[] iv = new byte[IV_LENGTH];
    SECURE_RANDOM.nextBytes(iv);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), gcmSpec);

    byte[] ciphertext = cipher.doFinal(plaintext);

    // Assemble `[0x01] + [12B IV] + [ciphertext + 16B authTag]`.
    byte[] result = new byte[1 + IV_LENGTH + ciphertext.length];
    result[0] = FORMAT_VERSION;
    System.arraycopy(iv, 0, result, 1, IV_LENGTH);
    System.arraycopy(ciphertext, 0, result, 1 + IV_LENGTH, ciphertext.length);

    return result;
  }

  /// @throws CredentialIntegrityException if the version is unsupported
  private byte[] decryptFileContent(byte[] fileContent, Path filePath)
      throws CredentialIntegrityException,
          NoSuchPaddingException,
          NoSuchAlgorithmException,
          InvalidAlgorithmParameterException,
          InvalidKeyException,
          IllegalBlockSizeException,
          BadPaddingException {
    byte version = fileContent[0];
    if (version != FORMAT_VERSION) {
      throw new CredentialIntegrityException(
          "Unsupported format version: " + version + " in file: " + filePath);
    }

    byte[] iv = new byte[IV_LENGTH];
    System.arraycopy(fileContent, 1, iv, 0, IV_LENGTH);

    int ciphertextLength = fileContent.length - 1 - IV_LENGTH;
    byte[] ciphertextWithTag = new byte[ciphertextLength];
    System.arraycopy(fileContent, 1 + IV_LENGTH, ciphertextWithTag, 0, ciphertextLength);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
    cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), gcmSpec);

    return cipher.doFinal(ciphertextWithTag);
  }

  // endregion

  // region File I/O helpers

  private void ensureDirectoryExists() throws IOException {
    try {
      Files.createDirectories(getDirPath(), DIR_ATTRIBUTE);
    } catch (UnsupportedOperationException e) {
      Files.createDirectories(getDirPath());
    }
    applyDirPermissions(getDirPath());
  }

  private static void validateFileSize(byte[] fileContent, Path filePath) {
    if (fileContent.length < MIN_FILE_SIZE) {
      throw new CredentialIntegrityException(
          "Credential file too short (" + fileContent.length + " bytes): " + filePath);
    }
  }

  // endregion

  // region Permission control

  private static void applyFilePermissions(Path path) throws IOException {
    try {
      Files.setPosixFilePermissions(path, FILE_PERMISSIONS);
    } catch (UnsupportedOperationException ignored) {
    }
  }

  private static void applyDirPermissions(Path path) throws IOException {
    try {
      Files.setPosixFilePermissions(path, DIR_PERMISSIONS);
    } catch (UnsupportedOperationException ignored) {
    }
  }

  // endregion
}
