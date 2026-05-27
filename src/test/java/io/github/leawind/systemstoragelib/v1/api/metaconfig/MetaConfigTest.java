package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MetaConfigTest extends BaseTest {
  private MetaConfig config;

  @BeforeEach
  void setupEach() throws IOException {
    config = lib.metaConfig().get();
  }

  @Nested
  class GetOrCreateScopeConfig {
    @Test
    void createsNewConfigWhenAbsent() {
      ScopeMetaConfig perScope = config.scope("new-scope");

      assertNotNull(perScope);
      assertTrue(config.scopes().containsKey("new-scope"));
    }

    @Test
    void returnsExistingConfigWhenPresent() {
      ScopeMetaConfig first = config.scope("scope-a");
      ScopeMetaConfig second = config.scope("scope-a");

      assertEquals(first, second);
    }
  }

  @Nested
  class RemoveScopeConfig {
    @Test
    void removesExistingScope() {
      config.scope("scope-a");
      config.scopes().remove("scope-a");

      assertTrue(config.scopes().isEmpty());
    }

    @Test
    void doesNothingForNonExistentScope() {
      config.scope("scope-a");
      config.scopes().remove("nonexistent");

      assertEquals(1, config.scopes().size());
    }
  }

  @Nested
  class GetScopeConfig {
    @Test
    void returnsNullForNonExistentScope() {
      assertNull(config.scopes().get("nonexistent"));
    }

    @Test
    void returnsConfigForExistingScope() {
      ScopeMetaConfig created = config.scope("scope-a");
      assertEquals(created, config.scopes().get("scope-a"));
    }
  }
}
