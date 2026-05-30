package io.github.leawind.systemstoragelib.v1.api.accessors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.exception.SecretIntegrityException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
                  // Min size: 1 (version) + 12 (IV) + 16 (auth tag) = 29
                  assertTrue(content.length >= 29, "Encrypted file should be at least 29 bytes");
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
}
