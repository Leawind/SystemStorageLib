package io.github.leawind.systemstoragelib.v1.impl.accessors;

import io.github.leawind.inventory.lock.AtomicFileWriter;
import io.github.leawind.systemstoragelib.v1.api.accessors.AbstractDirectoryAccessor;
import io.github.leawind.systemstoragelib.v1.api.accessors.SecretsAccessor;
import io.github.leawind.systemstoragelib.v1.api.exception.SecretIntegrityException;
import io.github.leawind.systemstoragelib.v1.utils.machineid.MachineIdResolutionException;
import io.github.leawind.systemstoragelib.v1.utils.machineid.MachineIdUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
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
import org.slf4j.Logger;

/// AES-256-GCM encrypted secret storage with environment-bound key derivation and expiration
// support.
///
/// File names are `{sha256_hex}.enc` (e.g. `a1b2c3d4...e5f6.enc`);
/// Binary format per file:
///
/// | Offset | Size | Field |
/// |--|--|--|
/// | `0x00` | 1B | Version (`0x01`) |
/// | `0x01` | 8B | Expiration epoch seconds (big-endian long; `Instant.MAX` = no expiration) |
/// | `0x09` | 12B | IV / Nonce |
/// | `0x15` | NB | Ciphertext |
/// | EOF-16 | 16B | GCM Auth Tag |
///
/// AES key derived via PBKDF2 from `user.name:user.home:machineId`.
///
/// Expiration is stored in plaintext in the file header so it can be read without decryption.
/// The value is always encrypted using AES-256-GCM. Only the value part is encrypted; the
/// version and expiration are plaintext.
public class SecretsAccessorImpl extends AbstractDirectoryAccessor implements SecretsAccessor {

  private static final byte FORMAT_VERSION = 0x01;
  private static final int IV_LENGTH = 12;
  private static final int AUTH_TAG_LENGTH = 16;
  private static final int EXPIRATION_EPOCH_LENGTH = 8;
  private static final int MIN_FILE_SIZE =
      1 + EXPIRATION_EPOCH_LENGTH + IV_LENGTH + AUTH_TAG_LENGTH; // 37

  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int AES_KEY_LENGTH_BITS = 256;

  private static final String SALT = "SystemStorageLib-Secrets-v1";
  private static final String FILE_SUFFIX = ".enc";

  private static final Set<PosixFilePermission> DIR_PERMISSIONS =
      PosixFilePermissions.fromString("rwx------");
  private static final Set<PosixFilePermission> FILE_PERMISSIONS =
      PosixFilePermissions.fromString("rw-------");
  private static final FileAttribute<Set<PosixFilePermission>> DIR_ATTRIBUTE =
      PosixFilePermissions.asFileAttribute(DIR_PERMISSIONS);

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private volatile SecretKey aesKey;

  public SecretsAccessorImpl(Path dirPath, Logger logger) {
    super(dirPath, logger);
  }

  @Override
  public boolean exists(@NonNull String key) {
    return getExpiration(key).map(exp -> !Instant.now().isAfter(exp)).orElse(false);
  }

  @Override
  public void set(@NonNull String key, @NonNull String value, @NonNull Instant expiresAt)
      throws IOException {
    validateKey(key);
    Path filePath = keyToFilePath(key);

    try {
      ensureDirectoryExists();

      byte[] plaintext = value.getBytes(StandardCharsets.UTF_8);
      long expirationEpoch = expiresAt.getEpochSecond();
      byte[] encrypted = encrypt(plaintext, expirationEpoch);

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

  @Override
  public @NonNull Optional<SecretsAccessor.SecretEntry> getEntry(@NonNull String key)
      throws SecretIntegrityException {
    validateKey(key);
    Path filePath = keyToFilePath(key);
    if (!Files.exists(filePath)) {
      return Optional.empty();
    }

    try {
      byte[] fileContent = Files.readAllBytes(filePath);
      validateFileSize(fileContent, filePath);

      long expirationEpoch = readExpirationEpoch(fileContent);
      Instant expiresAt = null;
      if (expirationEpoch != Instant.MAX.getEpochSecond()) {
        expiresAt = Instant.ofEpochSecond(expirationEpoch);
        if (Instant.now().isAfter(expiresAt)) {
          Files.deleteIfExists(filePath);
          return Optional.empty();
        }
      }

      byte[] plaintext = decryptFileContent(fileContent);
      String value = new String(plaintext, StandardCharsets.UTF_8);
      return Optional.of(new SecretsAccessor.SecretEntry(value, expiresAt));
    } catch (IOException e) {
      throw new SecretIntegrityException("Failed to read secret file: " + filePath, e);
    } catch (InvalidAlgorithmParameterException
        | NoSuchPaddingException
        | IllegalBlockSizeException
        | NoSuchAlgorithmException
        | BadPaddingException
        | InvalidKeyException e) {
      throw new SecretIntegrityException("Failed to decrypt secret file: " + filePath, e);
    }
  }

  @Override
  public @NonNull Optional<Instant> getExpiration(@NonNull String key) {
    validateKey(key);
    Path filePath = keyToFilePath(key);
    if (!Files.exists(filePath)) {
      return Optional.empty();
    }
    try {
      byte[] content = Files.readAllBytes(filePath);
      if (content.length < MIN_FILE_SIZE || content[0] != FORMAT_VERSION) {
        return Optional.empty();
      }
      long epoch = readExpirationEpoch(content);
      return Optional.of(Instant.ofEpochSecond(epoch));
    } catch (IOException e) {
      getLogger().warn("Failed to read expiration for key: {}", key, e);
      return Optional.empty();
    }
  }

  @Override
  public int cleanup() throws IOException {
    if (!Files.exists(getDirPath())) {
      return 0;
    }
    int count = 0;
    try (Stream<Path> files = Files.list(getDirPath())) {
      List<Path> encFiles =
          files.filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX)).toList();
      for (Path file : encFiles) {
        try {
          byte[] content = Files.readAllBytes(file);
          if (content.length < MIN_FILE_SIZE || content[0] != FORMAT_VERSION) {
            continue;
          }
          long expirationEpoch = readExpirationEpoch(content);
          if (expirationEpoch != Instant.MAX.getEpochSecond()
              && Instant.now().isAfter(Instant.ofEpochSecond(expirationEpoch))) {
            Files.deleteIfExists(file);
            count++;
          }
        } catch (IOException e) {
          getLogger().warn("Failed to process file during cleanup: {}", file, e);
        }
      }
    }
    return count;
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

  // region Expiration epoch I/O

  private static void writeExpirationEpoch(byte[] buf, int offset, long value) {
    buf[offset] = (byte) (value >>> 56);
    buf[offset + 1] = (byte) (value >>> 48);
    buf[offset + 2] = (byte) (value >>> 40);
    buf[offset + 3] = (byte) (value >>> 32);
    buf[offset + 4] = (byte) (value >>> 24);
    buf[offset + 5] = (byte) (value >>> 16);
    buf[offset + 6] = (byte) (value >>> 8);
    buf[offset + 7] = (byte) (value);
  }

  private static long readExpirationEpoch(byte[] buf) {
    long result = 0;
    for (int i = 0; i < EXPIRATION_EPOCH_LENGTH; i++) {
      result = (result << 8) | (buf[1 + i] & 0xFF);
    }
    return result;
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
      String machineId = "";
      try {
        machineId = MachineIdUtil.getMachineId();
      } catch (MachineIdResolutionException e) {
        getLogger().error("Failed to get machine id", e);
      }

      String keyMaterial =
          System.getProperty("user.name") + ":" + System.getProperty("user.home") + ":" + machineId;
      byte[] salt = SALT.getBytes(StandardCharsets.UTF_8);

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

  private byte[] encrypt(byte[] plaintext, long expirationEpoch)
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

    int headerSize = 1 + EXPIRATION_EPOCH_LENGTH;
    byte[] result = new byte[headerSize + IV_LENGTH + ciphertext.length];
    result[0] = FORMAT_VERSION;
    writeExpirationEpoch(result, 1, expirationEpoch);
    System.arraycopy(iv, 0, result, headerSize, IV_LENGTH);
    System.arraycopy(ciphertext, 0, result, headerSize + IV_LENGTH, ciphertext.length);

    return result;
  }

  /// @throws SecretIntegrityException if the version is unsupported
  private byte[] decryptFileContent(byte[] fileContent)
      throws SecretIntegrityException,
          NoSuchPaddingException,
          NoSuchAlgorithmException,
          InvalidAlgorithmParameterException,
          InvalidKeyException,
          IllegalBlockSizeException,
          BadPaddingException {
    byte version = fileContent[0];
    if (version != FORMAT_VERSION) {
      throw new SecretIntegrityException("Unsupported format version: " + version);
    }

    int ivOffset = 1 + EXPIRATION_EPOCH_LENGTH;
    byte[] iv = new byte[IV_LENGTH];
    System.arraycopy(fileContent, ivOffset, iv, 0, IV_LENGTH);

    int ciphertextOffset = ivOffset + IV_LENGTH;
    int ciphertextLength = fileContent.length - ciphertextOffset;
    byte[] ciphertextWithTag = new byte[ciphertextLength];
    System.arraycopy(fileContent, ciphertextOffset, ciphertextWithTag, 0, ciphertextLength);

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

  private static void validateFileSize(byte[] fileContent, Path filePath)
      throws SecretIntegrityException {
    if (fileContent.length < MIN_FILE_SIZE) {
      throw new SecretIntegrityException(
          "Secret file too short (" + fileContent.length + " bytes): " + filePath);
    }
  }

  // endregion

  // region Permission control

  private static void applyFilePermissions(Path path) throws IOException {
    try {
      Files.setPosixFilePermissions(path, FILE_PERMISSIONS);
    } catch (UnsupportedOperationException ignored) {
      applyWindowsAcl(path, false);
    }
  }

  private static void applyDirPermissions(Path path) throws IOException {
    try {
      Files.setPosixFilePermissions(path, DIR_PERMISSIONS);
    } catch (UnsupportedOperationException ignored) {
      applyWindowsAcl(path, true);
    }
  }

  /// Applies owner-only access via ACL on file systems that do not support POSIX permissions.
  ///
  /// Preserves existing non-owner ACL entries and adds an owner entry granting read and write
  /// (and execute for directories), equivalent to `rw-------` for files or `rwx------`.
  private static void applyWindowsAcl(Path path, boolean isDir) throws IOException {
    AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
    if (view == null) {
      return;
    }

    UserPrincipal owner = view.getOwner();
    AclEntry ownerEntry =
        AclEntry.newBuilder()
            .setType(AclEntryType.ALLOW)
            .setPrincipal(owner)
            .setPermissions(
                isDir
                    ? new AclEntryPermission[] {
                      AclEntryPermission.READ_DATA,
                      AclEntryPermission.WRITE_DATA,
                      AclEntryPermission.EXECUTE
                    }
                    : new AclEntryPermission[] {
                      AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA
                    })
            .build();

    List<AclEntry> entries = new ArrayList<>();
    for (AclEntry existing : view.getAcl()) {
      if (!existing.principal().equals(owner)) {
        entries.add(existing);
      }
    }
    entries.add(ownerEntry);
    view.setAcl(entries);
  }

  // endregion
}
