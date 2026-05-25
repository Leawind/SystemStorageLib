package io.github.leawind.systemstoragelib.v1.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.leawind.systemstoragelib.v1.utils.machineid.MachineIdUtil;
import org.junit.jupiter.api.Test;

public class OsMachineIdUtilTest {
  @Test
  void test() {
    String id1 = MachineIdUtil.getMachineId();
    String id2 = MachineIdUtil.getMachineId();

    assertFalse(id1.isBlank());
    assertEquals(id1, id2);
  }
}
