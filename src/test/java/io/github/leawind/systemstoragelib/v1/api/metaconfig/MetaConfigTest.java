package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import io.github.leawind.systemstoragelib.v1.api.managers.MetaConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MetaConfigTest extends BaseTest {
  private MetaConfigManager manager;

  @BeforeEach
  void setupEach() {
    manager = lib.metaConfig();
  }

  @Nested
  class GetOrCreateScopeConfig {
    @Test
    void createsNewConfigWhenAbsent() {
      MetaConfig config = manager.createConfig();
      PerScopeConfig perScope =
          config.scopes().computeIfAbsent("new-scope", ignored -> config.createScopeConfig());

      assertNotNull(perScope);
      assertTrue(config.scopes().containsKey("new-scope"));
    }

    @Test
    void returnsExistingConfigWhenPresent() {
      MetaConfig config = manager.createConfig();
      PerScopeConfig first =
          config.scopes().computeIfAbsent("scope-a", ignored1 -> config.createScopeConfig());
      PerScopeConfig second =
          config.scopes().computeIfAbsent("scope-a", ignored -> config.createScopeConfig());

      assertEquals(first, second);
    }
  }

  @Nested
  class RemoveScopeConfig {
    @Test
    void removesExistingScope() {
      MetaConfig config = manager.createConfig();
      config.scopes().computeIfAbsent("scope-a", ignored -> config.createScopeConfig());
      config.scopes().remove("scope-a");

      assertTrue(config.scopes().isEmpty());
    }

    @Test
    void doesNothingForNonExistentScope() {
      MetaConfig config = manager.createConfig();
      config.scopes().computeIfAbsent("scope-a", ignored -> config.createScopeConfig());
      config.scopes().remove("nonexistent");

      assertEquals(1, config.scopes().size());
    }
  }

  @Nested
  class GetScopeConfig {
    @Test
    void returnsNullForNonExistentScope() {
      MetaConfig config = manager.createConfig();
      assertNull(config.scopes().get("nonexistent"));
    }

    @Test
    void returnsConfigForExistingScope() {
      MetaConfig config = manager.createConfig();
      PerScopeConfig created =
          config.scopes().computeIfAbsent("scope-a", ignored -> config.createScopeConfig());
      assertEquals(created, config.scopes().get("scope-a"));
    }
  }
}
