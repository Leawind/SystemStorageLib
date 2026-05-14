package io.github.leawind.systemstoragelib.v1.impl.utils.machineid;

import java.io.IOException;

public interface MachineIdProvider {
  String get() throws IOException, MachineIdResolutionException;

  static String getMachineId() throws MachineIdResolutionException {
    String cached = MachineIdCache.VALUE.get();
    if (cached != null) {
      return cached;
    }

    synchronized (MachineIdCache.class) {
      cached = MachineIdCache.VALUE.get();
      if (cached == null) {
        cached = MachineIdProviderImpl.resolve();
        MachineIdCache.VALUE.set(cached);
      }
    }
    return cached;
  }
}
