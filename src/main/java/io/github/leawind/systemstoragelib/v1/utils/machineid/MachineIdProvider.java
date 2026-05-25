package io.github.leawind.systemstoragelib.v1.utils.machineid;

import java.io.IOException;

interface MachineIdProvider {
  String get() throws IOException, MachineIdResolutionException;
}
