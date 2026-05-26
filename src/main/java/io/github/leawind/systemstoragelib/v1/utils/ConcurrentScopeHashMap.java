package io.github.leawind.systemstoragelib.v1.utils;

import io.github.leawind.inventory.util.ValidatingConcurrentHashMap;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import java.util.Objects;

public class ConcurrentScopeHashMap<V> extends ValidatingConcurrentHashMap<String, V> {

  private final SystemStorageLib lib;

  public ConcurrentScopeHashMap(SystemStorageLib lib) {
    this.lib = lib;
  }

  @Override
  public void validateEntry(String scopeName, V value)
      throws NullPointerException, IllegalArgumentException {
    Objects.requireNonNull(scopeName, "scope name must not be null");

    String err = lib.validateScopeName(scopeName);
    if (err != null) {
      throw new IllegalArgumentException("Invalid scope name: " + scopeName + ", " + err);
    }
  }
}
