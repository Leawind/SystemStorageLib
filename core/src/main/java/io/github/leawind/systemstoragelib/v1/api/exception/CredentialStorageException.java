package io.github.leawind.systemstoragelib.v1.api.exception;

/**
 * Thrown when a credential storage operation fails due to I/O errors or permission issues.
 *
 * <p>This exception wraps underlying {@link java.io.IOException IOExceptions} (such as {@link
 * java.nio.file.AccessDeniedException}) as unchecked exceptions, since the {@code CredentialStore}
 * API does not declare checked exceptions.
 */
public class CredentialStorageException extends RuntimeException {
  public CredentialStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
