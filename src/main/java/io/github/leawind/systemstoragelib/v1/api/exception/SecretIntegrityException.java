package io.github.leawind.systemstoragelib.v1.api.exception;

public class SecretIntegrityException extends Exception {
  public SecretIntegrityException(String message) {
    super(message);
  }

  public SecretIntegrityException(String message, Throwable cause) {
    super(message, cause);
  }
}
