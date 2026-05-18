package io.github.leawind.systemstoragelib.v1.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.systemstoragelib.v1.utils.machineid.MachineIdProvider;
import org.junit.jupiter.api.Test;

public class OsMachineIdProviderTest {
  @Test
  void test() {
    String id1 = MachineIdProvider.getMachineId();
    String id2 = MachineIdProvider.getMachineId();

    assertEquals(id1, id2);
  }
}
