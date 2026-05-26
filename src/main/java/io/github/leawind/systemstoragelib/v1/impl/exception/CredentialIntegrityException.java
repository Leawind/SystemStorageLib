package io.github.leawind.systemstoragelib.v1.impl.exception;

/// Thrown when credential data is corrupted, tampered with, or cannot be read.
public class CredentialIntegrityException extends RuntimeException {
  public CredentialIntegrityException(String message) {
    super(message);
  }

  public CredentialIntegrityException(String message, Throwable cause) {
    super(message, cause);
  }
}
