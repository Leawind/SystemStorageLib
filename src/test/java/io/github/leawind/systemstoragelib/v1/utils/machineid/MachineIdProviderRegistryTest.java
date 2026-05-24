package io.github.leawind.systemstoragelib.v1.utils.machineid;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class MachineIdProviderRegistryTest {

  @Test
  void forKeywordsRegistersProviderUnderEachKeyword() throws IOException {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();
    registry.forKeywords("a", "b", "c").register(() -> "x");

    assertEquals("x", registry.getProvider("a").get());
    assertEquals("x", registry.getProvider("b").get());
    assertEquals("x", registry.getProvider("c").get());
  }

  @Test
  void getProviderMatchesWhenOsContainsKeyword() throws IOException {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();
    registry.forKeywords("windows").register(() -> "win-id");

    assertEquals("win-id", registry.getProvider("Windows 10").get());
  }

  @Test
  void getProviderMatchesCaseInsensitively() throws IOException {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();
    registry.forKeywords("windows").register(() -> "win-id");

    assertEquals("win-id", registry.getProvider("WINDOWS 10").get());
    assertEquals("win-id", registry.getProvider("windows 10").get());
    assertEquals("win-id", registry.getProvider("Windows").get());
  }

  @Test
  void getProviderMatchesExactKeyword() throws IOException {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();
    registry.forKeywords("linux").register(() -> "linux-id");

    assertEquals("linux-id", registry.getProvider("linux").get());
  }

  @Test
  void getProviderThrowsOnUnknownOs() {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();
    registry.forKeywords("linux").register(() -> "linux-id");

    MachineIdResolutionException ex =
        assertThrows(MachineIdResolutionException.class, () -> registry.getProvider("freebsd"));
    assertTrue(ex.getMessage().contains("freebsd"));
  }

  @Test
  void getProviderThrowsOnEmptyRegistry() {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();

    assertThrows(MachineIdResolutionException.class, () -> registry.getProvider("linux"));
  }

  @Test
  void multipleKeywordsCanShareProvider() throws IOException {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();
    registry.forKeywords("linux", "unix").register(() -> "unix-id");

    assertEquals("unix-id", registry.getProvider("linux").get());
    assertEquals("unix-id", registry.getProvider("unix").get());
  }

  @Test
  void getInstanceReturnsSingleton() {
    MachineIdProviderRegistry r1 = MachineIdProviderRegistry.getInstance();
    MachineIdProviderRegistry r2 = MachineIdProviderRegistry.getInstance();
    assertEquals(r1, r2);
  }

  @Test
  void registeredProviderCanThrow() {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();
    registry
        .forKeywords("linux")
        .register(
            () -> {
              throw new IOException("simulated failure");
            });

    assertThrows(IOException.class, () -> registry.getProvider("linux").get());
  }

  @Test
  void getProviderDoesNotThrowForRegisteredOs() {
    MachineIdProviderRegistry registry = new MachineIdProviderRegistry();
    registry.forKeywords("mac").register(() -> "mac-id");

    assertDoesNotThrow(() -> registry.getProvider("Mac OS X"));
  }
}
