package io.github.leawind.systemstoragelib.v1.api;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SystemStorageLibTest {
  public static final String SCOPE_ID = "system_storage_lib_test";

  public static final ScopeStorage SCOPE = SystemStorageLib.getInstance().scope(SCOPE_ID);

  @Test
  void testDifferentDirs() {
    Set<Path> paths = new HashSet<>();
    paths.add(SystemStorageLib.getInstance().getLogsDir());

    assertFalse(paths.contains(SCOPE.credentials().getDirPath()));
    paths.add(SCOPE.credentials().getDirPath());

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
