package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MetaConfigTest {

  @Nested
  class GetOrCreateScopeConfig {
    @Test
    void createsNewConfigWhenAbsent() {
      MetaConfig config = MetaConfig.getDefault();
      PerScopeConfig perScope = config.getScopeConfig("new-scope");

      assertNotNull(perScope);
      assertTrue(config.scopes().containsKey("new-scope"));
    }

    @Test
    void returnsExistingConfigWhenPresent() {
      MetaConfig config = MetaConfig.getDefault();
      PerScopeConfig first = config.getScopeConfig("scope-a");
      PerScopeConfig second = config.getScopeConfig("scope-a");

      assertEquals(first, second);
    }
  }

  @Nested
  class RemoveScopeConfig {
    @Test
    void removesExistingScope() {
      MetaConfig config = MetaConfig.getDefault();
      config.getScopeConfig("scope-a");
      config.scopes().remove("scope-a");

      assertTrue(config.scopes().isEmpty());
    }

    @Test
    void doesNothingForNonExistentScope() {
      MetaConfig config = MetaConfig.getDefault();
      config.getScopeConfig("scope-a");
      config.scopes().remove("nonexistent");

      assertEquals(1, config.scopes().size());
    }
  }

  @Nested
  class GetScopeConfig {
    @Test
    void returnsNullForNonExistentScope() {
      MetaConfig config = MetaConfig.getDefault();
      assertNull(config.scopes().get("nonexistent"));
    }

    @Test
    void returnsConfigForExistingScope() {
      MetaConfig config = MetaConfig.getDefault();
      PerScopeConfig created = config.getScopeConfig("scope-a");
      assertEquals(created, config.scopes().get("scope-a"));
    }
  }
}
