package io.github.leawind.systemstoragelib.v1.api.metaconfig;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;

public class MetaConfigTest extends BaseTest {
  private MetaConfig config;

  @BeforeEach
  void setupEach() throws IOException {
    config = lib.metaConfig().get();
  }
}
