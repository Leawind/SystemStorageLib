package io.github.leawind.systemstoragelib.v1.utils.machineid;

import java.util.HashMap;
import java.util.Map;

class MachineIdProviderRegistry {
  private static final MachineIdProviderRegistry INSTANCE = new MachineIdProviderRegistry();

  static MachineIdProviderRegistry getInstance() {
    return INSTANCE;
  }

  private final Map<String, MachineIdProvider> providers = new HashMap<>();

  MachineIdProviderRegistry() {}

  Registrar forKeywords(String... keyword) {
    return provider -> {
      for (String k : keyword) {
        providers.put(k, provider);
      }
    };
  }

  MachineIdProvider getProvider(String os) {
    os = os.toLowerCase();
    for (Map.Entry<String, MachineIdProvider> entry : providers.entrySet()) {
      if (os.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    throw new MachineIdResolutionException("Unsupported OS: " + os);
  }

  interface Registrar {
    void register(MachineIdProvider provider);
  }
}
