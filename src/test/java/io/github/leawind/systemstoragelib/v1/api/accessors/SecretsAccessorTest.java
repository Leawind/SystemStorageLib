package io.github.leawind.systemstoragelib.v1.api.accessors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.accessors.SecretsAccessor.SecretEntry;
import io.github.leawind.systemstoragelib.v1.api.exception.SecretIntegrityException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SecretsAccessorTest extends BaseTest {

  private SecretsAccessor secrets;

  @BeforeEach
  void setUp() {
    secrets = scope.access(StoreType.SECRETS, SecretsAccessor::from);
  }

  @Test
  void testSetAndGet() throws IOException, SecretIntegrityException {
    secrets.set("github_token", "secret_value_123");
    assertEquals("secret_value_123", secrets.get("github_token"));
  }

  @Test
  void testGetNonExistent() throws SecretIntegrityException {
    assertNull(secrets.get("non_existent_key"));
  }

  @Test
  void testExists() throws IOException {
    assertFalse(secrets.exists("api_key"));
    secrets.set("api_key", "value");
    assertTrue(secrets.exists("api_key"));
  }

  @Test
  void testRemove() throws IOException {
    secrets.set("temp_key", "temp_value");
    assertTrue(secrets.exists("temp_key"));
    secrets.remove("temp_key");
    assertFalse(secrets.exists("temp_key"));
  }

  @Test
  void testRemoveNonExistent() throws IOException {
    secrets.remove("non_existent");
  }

  @Test
  void testOverwrite() throws IOException, SecretIntegrityException {
    secrets.set("key", "old_value");
    assertEquals("old_value", secrets.get("key"));
    secrets.set("key", "new_value");
    assertEquals("new_value", secrets.get("key"));
  }

  @Test
  void testEmptyValue() throws IOException, SecretIntegrityException {
    secrets.set("empty_key", "");
    assertEquals("", secrets.get("empty_key"));
  }

  @Test
  void testUnicodeValue() throws IOException, SecretIntegrityException {
    String unicode = "密码🔑セキュリティ";
    secrets.set("unicode_key", unicode);
    assertEquals(unicode, secrets.get("unicode_key"));
  }

  @Test
  void testEmptyKeyRejected() {
    assertThrows(IllegalArgumentException.class, () -> secrets.set("", "value"));
  }

  @Test
  void testFileNamingIsHashed() throws IOException {
    secrets.set("github_token", "value");
    try (Stream<Path> paths = Files.list(secrets.getDirPath())) {
      List<Path> encFiles = paths.filter(p -> p.toString().endsWith(".enc")).toList();
      assertEquals(1, encFiles.size());
      for (Path p : encFiles) {
        String name = p.getFileName().toString();
        assertTrue(name.matches("[0-9a-f]+\\.enc"), "File name should be hex hash: " + name);
        assertFalse(name.contains("github_token"), "File name should not contain the key name");
      }
    }
  }

  @Test
  void testFileContentIsEncrypted() throws IOException {
    String secretValue = "this_is_a_secret_that_should_not_appear_in_plaintext";
    secrets.set("secret_key", secretValue);
    try (Stream<Path> paths = Files.list(secrets.getDirPath())) {
      paths
          .filter(p -> p.toString().endsWith(".enc"))
          .forEach(
              p -> {
                try {
                  byte[] content = Files.readAllBytes(p);
                  String contentStr = new String(content);
                  assertFalse(
                      contentStr.contains(secretValue),
                      "File content should not contain the plaintext value");
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  void testFileMinSize() throws IOException {
    secrets.set("key", "x");
    try (Stream<Path> paths = Files.list(secrets.getDirPath())) {
      paths
          .filter(p -> p.toString().endsWith(".enc"))
          .forEach(
              p -> {
                try {
                  byte[] content = Files.readAllBytes(p);
                  // Min size: 1 (version) + 8 (expiration) + 12 (IV) + 16 (auth tag) = 37
                  assertTrue(content.length >= 37, "Encrypted file should be at least 37 bytes");
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  void testCorruptedFileThrowsIntegrityException() throws IOException {
    secrets.set("corrupt_key", "value");
    try (Stream<Path> paths = Files.list(secrets.getDirPath())) {
      paths
          .filter(p -> p.toString().endsWith(".enc"))
          .findFirst()
          .ifPresent(
              p -> {
                try {
                  byte[] content = Files.readAllBytes(p);
                  // Corrupt the auth tag (last 16 bytes)
                  content[content.length - 1] ^= (byte) 0xFF;
                  Files.write(p, content);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
    assertThrows(SecretIntegrityException.class, () -> secrets.get("corrupt_key"));
  }

  @Test
  void testMultipleKeysIsolation() throws IOException, SecretIntegrityException {
    secrets.set("key_a", "value_a");
    secrets.set("key_b", "value_b");
    assertEquals("value_a", secrets.get("key_a"));
    assertEquals("value_b", secrets.get("key_b"));
  }

  @Test
  void testDirPathIsAccessible() {
    assertNotNull(secrets.getDirPath());
  }

  @Test
  void testLongValue() throws IOException, SecretIntegrityException {
    String longValue = "A".repeat(10000);
    secrets.set("long_key", longValue);
    assertEquals(longValue, secrets.get("long_key"));
  }

  // region Expiration tests

  @Test
  void testSetWithExpiration() throws IOException, SecretIntegrityException {
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
    secrets.set("expiring_key", "secret_data", expiresAt);
    Optional<SecretEntry> entry = secrets.getEntry("expiring_key");
    assertTrue(entry.isPresent());
    assertEquals("secret_data", entry.get().value());
    assertNotNull(entry.get().expiresAt());
    assertFalse(entry.get().isExpired());
  }

  @Test
  void testSetWithNoExpiration() throws IOException, SecretIntegrityException {
    secrets.set("permanent_key", "forever_value");
    Optional<SecretEntry> entry = secrets.getEntry("permanent_key");
    assertTrue(entry.isPresent());
    assertEquals("forever_value", entry.get().value());
    assertTrue(entry.get().expiresAt() == null || !entry.get().isExpired());
  }

  @Test
  void testExpiredSecretNotExists() throws IOException {
    Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
    secrets.set("expired_key", "expired_value", past);
    assertFalse(secrets.exists("expired_key"));
  }

  @Test
  void testExpiredSecretGetReturnsNull() throws IOException, SecretIntegrityException {
    Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
    secrets.set("expired_get_key", "expired_value", past);
    assertNull(secrets.get("expired_get_key"));
  }

  @Test
  void testExpiredSecretGetEntryReturnsEmpty() throws IOException, SecretIntegrityException {
    Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
    secrets.set("expired_entry_key", "expired_value", past);
    assertTrue(secrets.getEntry("expired_entry_key").isEmpty());
  }

  @Test
  void testGetExpiration() throws IOException {
    Instant expiresAt = Instant.now().plus(2, ChronoUnit.HOURS);
    secrets.set("exp_key", "value", expiresAt);
    Optional<Instant> expiration = secrets.getExpiration("exp_key");
    assertTrue(expiration.isPresent());
    long diffSeconds = Math.abs(expiration.get().getEpochSecond() - expiresAt.getEpochSecond());
    assertTrue(diffSeconds <= 2, "Expiration should be within 2 seconds of what was set");
  }

  @Test
  void testGetExpirationNonExistent() {
    Optional<Instant> expiration = secrets.getExpiration("non_existent_exp_key");
    assertTrue(expiration.isEmpty());
  }

  @Test
  void testUpdateExpiration() throws IOException, SecretIntegrityException {
    Instant original = Instant.now().plus(1, ChronoUnit.HOURS);
    secrets.set("update_exp_key", "value", original);
    Instant newExpiration = Instant.now().plus(3, ChronoUnit.HOURS);
    secrets.updateExpiration("update_exp_key", newExpiration);
    Optional<Instant> expiration = secrets.getExpiration("update_exp_key");
    assertTrue(expiration.isPresent());
    long diffSeconds = Math.abs(expiration.get().getEpochSecond() - newExpiration.getEpochSecond());
    assertTrue(diffSeconds <= 2);
  }

  @Test
  void testMakePermanent() throws IOException, SecretIntegrityException {
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
    secrets.set("make_perm_key", "value", expiresAt);
    secrets.makePermanent("make_perm_key");
    Optional<SecretEntry> entry = secrets.getEntry("make_perm_key");
    assertTrue(entry.isPresent());
    assertFalse(entry.get().isExpired());
  }

  @Test
  void testCleanupRemovesExpired() throws IOException {
    Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
    Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
    secrets.set("cleanup_expired_1", "val1", past);
    secrets.set("cleanup_expired_2", "val2", past);
    secrets.set("cleanup_valid", "val3", future);
    secrets.set("cleanup_permanent", "val4");
    int cleaned = secrets.cleanup();
    assertEquals(2, cleaned);
    assertFalse(secrets.exists("cleanup_expired_1"));
    assertFalse(secrets.exists("cleanup_expired_2"));
    assertTrue(secrets.exists("cleanup_valid"));
    assertTrue(secrets.exists("cleanup_permanent"));
  }

  @Test
  void testCleanupEmptyDirectory() throws IOException {
    int cleaned = secrets.cleanup();
    assertEquals(0, cleaned);
  }

  @Test
  void testSetWithDuration() throws IOException, SecretIntegrityException {
    secrets.set("duration_key", "duration_value", Duration.ofHours(2));
    Optional<SecretEntry> entry = secrets.getEntry("duration_key");
    assertTrue(entry.isPresent());
    assertEquals("duration_value", entry.get().value());
    assertNotNull(entry.get().expiresAt());
    assertFalse(entry.get().isExpired());
  }

  @Test
  void testUpdateExpirationWithDuration() throws IOException, SecretIntegrityException {
    secrets.set("update_dur_key", "value", Duration.ofHours(1));
    secrets.updateExpiration("update_dur_key", Duration.ofHours(5));
    Optional<SecretEntry> entry = secrets.getEntry("update_dur_key");
    assertTrue(entry.isPresent());
    assertNotNull(entry.get().expiresAt());
    assertFalse(entry.get().isExpired());
  }

  @Test
  void testSecretEntryIsExpiredPast() {
    SecretEntry expiredEntry = new SecretEntry("value", Instant.now().minus(1, ChronoUnit.HOURS));
    assertTrue(expiredEntry.isExpired());
  }

  @Test
  void testSecretEntryIsExpiredFuture() {
    SecretEntry activeEntry = new SecretEntry("value", Instant.now().plus(1, ChronoUnit.HOURS));
    assertFalse(activeEntry.isExpired());
  }

  @Test
  void testSecretEntryValueGetter() {
    SecretEntry entry = new SecretEntry("test_value", Instant.now());
    assertEquals("test_value", entry.value());
  }

  // endregion
}
