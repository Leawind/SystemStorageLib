package io.github.leawind.systemstoragelib.v1.api.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CredentialStoreTest {

  @TempDir Path tempDir;

  private CredentialStore store;

  @BeforeEach
  void setUp() {
    store = CredentialStore.of(tempDir.resolve("credentials"));
  }

  @Test
  void testSetAndGet() throws IOException {
    store.set("github_token", "secret_value_123");
    assertEquals("secret_value_123", store.get("github_token"));
  }

  @Test
  void testGetNonExistent() throws IOException {
    assertNull(store.get("non_existent_key"));
  }

  @Test
  void testExists() throws IOException {
    assertFalse(store.exists("api_key"));
    store.set("api_key", "value");
    assertTrue(store.exists("api_key"));
  }

  @Test
  void testRemove() throws IOException {
    store.set("temp_key", "temp_value");
    assertTrue(store.exists("temp_key"));
    store.remove("temp_key");
    assertFalse(store.exists("temp_key"));
  }

  @Test
  void testRemoveNonExistent() throws IOException {
    store.remove("non_existent");
  }

  @Test
  void testOverwrite() throws IOException {
    store.set("key", "old_value");
    assertEquals("old_value", store.get("key"));
    store.set("key", "new_value");
    assertEquals("new_value", store.get("key"));
  }

  @Test
  void testEmptyValue() throws IOException {
    store.set("empty_key", "");
    assertEquals("", store.get("empty_key"));
  }

  @Test
  void testUnicodeValue() throws IOException {
    String unicode = "密码🔑セキュリティ";
    store.set("unicode_key", unicode);
    assertEquals(unicode, store.get("unicode_key"));
  }

  @Test
  void testEmptyKeyRejected() {
    assertThrows(IllegalArgumentException.class, () -> store.set("", "value"));
  }

  @Test
  void testFileNamingIsHashed() throws IOException {
    store.set("github_token", "value");
    try (var paths = Files.list(store.getDirPath())) {
      var encFiles = paths.filter(p -> p.toString().endsWith(".enc")).toList();
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
    store.set("secret_key", secretValue);
    try (var paths = Files.list(store.getDirPath())) {
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
    store.set("key", "x");
    try (var paths = Files.list(store.getDirPath())) {
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
    store.set("corrupt_key", "value");
    try (var paths = Files.list(store.getDirPath())) {
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
    assertThrows(RuntimeException.class, () -> store.get("corrupt_key"));
  }

  @Test
  void testMultipleKeysIsolation() throws IOException {
    store.set("key_a", "value_a");
    store.set("key_b", "value_b");
    assertEquals("value_a", store.get("key_a"));
    assertEquals("value_b", store.get("key_b"));
  }

  @Test
  void testClearRemovesCredentials() throws IOException {
    store.set("key1", "val1");
    store.set("key2", "val2");
    store.clear();
    assertFalse(store.exists("key1"));
    assertFalse(store.exists("key2"));
  }

  @Test
  void testDirPathIsAccessible() {
    assertNotNull(store.getDirPath());
  }

  @Test
  void testLongValue() throws IOException {
    String longValue = "A".repeat(10000);
    store.set("long_key", longValue);
    assertEquals(longValue, store.get("long_key"));
  }
}
