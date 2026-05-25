package io.github.leawind.systemstoragelib.v1.api.exception;

import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;

/// Thrown when {@link SystemStorageLib} initialization fails.
public class SystemStorageLibException extends RuntimeException {
  public SystemStorageLibException(String message, Throwable cause) {
    super(message, cause);
  }
}
