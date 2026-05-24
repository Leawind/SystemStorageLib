package io.github.leawind.systemstoragelib.v1.utils.machineid;

import java.util.HashMap;
import java.util.Map;

public class MachineIdProviderRegistry {
  private static final MachineIdProviderRegistry INSTANCE = new MachineIdProviderRegistry();

  private final Map<String, MachineIdProvider> providers = new HashMap<>();

  public MachineIdProviderRegistry() {}

  public Registrar forKeywords(String... keyword) {
    return provider -> {
      for (String k : keyword) {
        providers.put(k, provider);
      }
    };
  }

  public MachineIdProvider getProvider(String os) {
    os = os.toLowerCase();
    for (Map.Entry<String, MachineIdProvider> entry : providers.entrySet()) {
      if (os.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    throw new MachineIdResolutionException("Unsupported OS: " + os);
  }

  public interface Registrar {
    void register(MachineIdProvider provider);
  }

  public static MachineIdProviderRegistry getInstance() {
    return INSTANCE;
  }
}
