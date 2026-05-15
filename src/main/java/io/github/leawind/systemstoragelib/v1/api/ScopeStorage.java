package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import org.slf4j.Logger;

public interface ScopeStorage {
  String scope();

  Logger logger();

  <S extends StorageManager> S storage(StoreType<S> storeType);
}
