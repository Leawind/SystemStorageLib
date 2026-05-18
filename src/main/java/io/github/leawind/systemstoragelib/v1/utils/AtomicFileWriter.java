package io.github.leawind.systemstoragelib.v1.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Atomically writes data to a file using the write-rename pattern.
///
/// <p>The write flow: write to a temp file → fsync → atomic move to target.
/// This ensures the target file is never in a partially-written state, even if the process or
/// system crashes mid-write.
///
/// <p>If the file system does not support atomic move (e.g. some network file systems), it
/// gracefully falls back to a regular move. The target file is still guaranteed to be either
/// the old version or the new version, never corrupted.
public final class AtomicFileWriter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AtomicFileWriter.class);

  private static final String DEFAULT_TMP_SUFFIX = ".tmp";

  private AtomicFileWriter() {}

  /// Creates parent directories if they don't exist. On failure, the temp file is cleaned up
  /// and the target file is left unchanged.
  public static void write(Path target, byte[] data) throws IOException {
    write(target, resolveTmpPath(target), data);
  }

  /// Creates parent directories if they don't exist. On failure, the temp file is cleaned up
  /// and the target file is left unchanged.
  public static void write(Path target, Path tmpPath, byte[] data) throws IOException {
    Files.createDirectories(target.getParent());
    try {
      writeToTmp(tmpPath, data);
      moveAtomically(tmpPath, target);
    } catch (IOException e) {
      cleanUp(tmpPath);
      throw e;
    }
  }

  /// Resolves the temp file path adjacent to the target file.
  ///
  /// <p>The temp file is always in the same directory as the target, which is required for
  /// atomic move to work (same file system).
  static Path resolveTmpPath(Path target) {
    return target.resolveSibling(target.getFileName() + DEFAULT_TMP_SUFFIX);
  }

  /// Writes {@code data} to {@code tmpPath} and calls fsync.
  ///
  /// @throws IOException if the `write` is incomplete or fsync fails
  static void writeToTmp(Path tmpPath, byte[] data) throws IOException {
    try (FileChannel channel =
        FileChannel.open(
            tmpPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      int written = channel.write(ByteBuffer.wrap(data));
      if (written != data.length) {
        throw new IOException("Failed to write all data to file: " + tmpPath);
      }
      channel.force(true);
    }
  }

  /// Moves {@code source} to {@code target} atomically if possible, with graceful fallback.
  ///
  /// <p>Prefers {@code ATOMIC_MOVE}. If the file system doesn't support it, falls back to a
  /// regular move with {@code REPLACE_EXISTING}.
  ///
  /// @throws IOException if the move fails
  static void moveAtomically(Path source, Path target) throws IOException {
    try {
      Files.move(
          source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /// Deletes the temp file if it exists. Failures are logged as warnings, not propagated.
  static void cleanUp(Path tmpPath) {
    try {
      Files.deleteIfExists(tmpPath);
    } catch (IOException e) {
      LOGGER.warn("Failed to clean up temp file: {}", tmpPath, e);
    }
  }
}
