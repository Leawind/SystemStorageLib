package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MetaConfigTest {

  @Nested
  class ScopeSet {
    @Test
    void defaultConfigReturnsEmptySet() {
      MetaConfig config = MetaConfig.getDefault();
      assertTrue(config.scopeSet().isEmpty());
    }

    @Test
    void returnsAllScopeNames() {
      MetaConfig config = MetaConfig.getDefault();
      config.getOrCreateScopeConfig("scope-a");
      config.getOrCreateScopeConfig("scope-b");

      Set<String> scopes = config.scopeSet();

      assertEquals(2, scopes.size());
      assertTrue(scopes.contains("scope-a"));
      assertTrue(scopes.contains("scope-b"));
    }

    @Test
    void isUnmodifiable() {
      MetaConfig config = MetaConfig.getDefault();
      config.getOrCreateScopeConfig("scope-a");

      assertThrows(UnsupportedOperationException.class, () -> config.scopeSet().add("scope-b"));
    }

    @Test
    void reflectsRemoval() {
      MetaConfig config = MetaConfig.getDefault();
      config.getOrCreateScopeConfig("scope-a");
      config.getOrCreateScopeConfig("scope-b");

      config.removeScopeConfig("scope-a");

      assertEquals(1, config.scopeSet().size());
      assertTrue(config.scopeSet().contains("scope-b"));
    }
  }

  @Nested
  class EntrySet {
    @Test
    void defaultConfigReturnsEmptyEntrySet() {
      MetaConfig config = MetaConfig.getDefault();
      assertTrue(config.entrySet().isEmpty());
    }

    @Test
    void returnsAllEntries() {
      MetaConfig config = MetaConfig.getDefault();
      config.getOrCreateScopeConfig("scope-a");
      config.getOrCreateScopeConfig("scope-b");

      Set<Map.Entry<String, PerScopeConfig>> entries = config.entrySet();

      assertEquals(2, entries.size());
      List<String> scopeNames = entries.stream().map(Map.Entry::getKey).toList();
      assertTrue(scopeNames.contains("scope-a"));
      assertTrue(scopeNames.contains("scope-b"));
    }

    @Test
    void entriesContainCorrectValues() {
      MetaConfig config = MetaConfig.getDefault();
      PerScopeConfig perScope = config.getOrCreateScopeConfig("scope-a");

      Set<Map.Entry<String, PerScopeConfig>> entries = config.entrySet();
      Optional<Map.Entry<String, PerScopeConfig>> entry =
          entries.stream().filter(e -> e.getKey().equals("scope-a")).findFirst();

      assertTrue(entry.isPresent());
      assertEquals(perScope, entry.get().getValue());
    }

    @Test
    void isUnmodifiable() {
      MetaConfig config = MetaConfig.getDefault();
      config.getOrCreateScopeConfig("scope-a");

      assertThrows(UnsupportedOperationException.class, () -> config.entrySet().clear());
    }
  }

  @Nested
  class GetOrCreateScopeConfig {
    @Test
    void createsNewConfigWhenAbsent() {
      MetaConfig config = MetaConfig.getDefault();
      PerScopeConfig perScope = config.getOrCreateScopeConfig("new-scope");

      assertNotNull(perScope);
      assertTrue(config.scopeSet().contains("new-scope"));
    }

    @Test
    void returnsExistingConfigWhenPresent() {
      MetaConfig config = MetaConfig.getDefault();
      PerScopeConfig first = config.getOrCreateScopeConfig("scope-a");
      PerScopeConfig second = config.getOrCreateScopeConfig("scope-a");

      assertEquals(first, second);
    }
  }

  @Nested
  class RemoveScopeConfig {
    @Test
    void removesExistingScope() {
      MetaConfig config = MetaConfig.getDefault();
      config.getOrCreateScopeConfig("scope-a");
      config.removeScopeConfig("scope-a");

      assertTrue(config.scopeSet().isEmpty());
    }

    @Test
    void doesNothingForNonExistentScope() {
      MetaConfig config = MetaConfig.getDefault();
      config.getOrCreateScopeConfig("scope-a");
      config.removeScopeConfig("nonexistent");

      assertEquals(1, config.scopeSet().size());
    }
  }

  @Nested
  class GetScopeConfig {
    @Test
    void returnsNullForNonExistentScope() {
      MetaConfig config = MetaConfig.getDefault();
      assertNull(config.getScopeConfig("nonexistent"));
    }

    @Test
    void returnsConfigForExistingScope() {
      MetaConfig config = MetaConfig.getDefault();
      PerScopeConfig created = config.getOrCreateScopeConfig("scope-a");
      assertEquals(created, config.getScopeConfig("scope-a"));
    }
  }
}
