package io.github.leawind.systemstoragelib.v1.utils.machineid;

import java.util.concurrent.atomic.AtomicReference;

public class MachineIdCache {
  static final AtomicReference<String> VALUE = new AtomicReference<>();
}
