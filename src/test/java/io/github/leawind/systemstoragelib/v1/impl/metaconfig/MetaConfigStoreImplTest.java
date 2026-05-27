package io.github.leawind.systemstoragelib.v1.impl.metaconfig;

import io.github.leawind.systemstoragelib.v1.BaseTest;
import org.junit.jupiter.api.BeforeEach;

public class MetaConfigStoreImplTest extends BaseTest {

  MetaConfigStoreImpl store;

  @BeforeEach
  void setup() {
    store = new MetaConfigStoreImpl(lib, lib.metaConfig().storage());
  }
}
