package io.github.leawind.systemstoragelib.v1.api;

import io.github.leawind.systemstoragelib.v1.impl.SystemStorageLibImpl;

final class SystemStorageLibHolder {
  static final SystemStorageLib INSTANCE = new SystemStorageLibImpl();
}
