package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SystemStorageLibTest {
  public static final String SCOPE_ID = "system_storage_lib_test";

  public static final SystemStorageLib LIB = SystemStorageLib.getInstance();
  public static final ScopeStorage SCOPE = LIB.getScopeStorage(SCOPE_ID);

  @Test
  void testDifferentDirs() {
    Set<Path> paths = new HashSet<>();
    paths.add(LIB.getLogDir());

    assertFalse(paths.contains(SCOPE.credentialStore().getDirPath()));
    paths.add(SCOPE.credentialStore().getDirPath());

    assertFalse(paths.contains(SCOPE.data().getDirPath()));
    paths.add(SCOPE.data().getDirPath());

    assertFalse(paths.contains(SCOPE.config().getDirPath()));
    paths.add(SCOPE.config().getDirPath());

    assertFalse(paths.contains(SCOPE.cache().getDirPath()));
    paths.add(SCOPE.cache().getDirPath());

    assertFalse(paths.contains(SCOPE.dataLocal().getDirPath()));
    paths.add(SCOPE.dataLocal().getDirPath());
  }
}
