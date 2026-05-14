package io.github.leawind.systemstoragelib.v1.impl;

import io.github.leawind.systemstoragelib.v1.api.ScopeStorage;
import io.github.leawind.systemstoragelib.v1.api.managers.CredentialStore;
import io.github.leawind.systemstoragelib.v1.api.managers.StorageManager;
import org.slf4j.Logger;

public record ScopeStorageImpl(
    String scope,
    Logger logger,
    CredentialStore credentials,
    StorageManager data,
    StorageManager config,
    StorageManager cache,
    StorageManager dataLocal)
    implements ScopeStorage {}
