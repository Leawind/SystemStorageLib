package io.github.leawind.systemstoragelib.v1.api.exception;

public class CredentialIntegrityException extends Exception {
  public CredentialIntegrityException(String message) {
    super(message);
  }

  public CredentialIntegrityException(String message, Throwable cause) {
    super(message, cause);
  }
}
