package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.managers.CredentialStore;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import org.slf4j.Logger;

public interface ScopeStorage {
  String scope();

  Logger logger();

  CredentialStore credentialStore();

  StorageManager data();

  StorageManager config();

  StorageManager cache();

  StorageManager dataLocal();
}
