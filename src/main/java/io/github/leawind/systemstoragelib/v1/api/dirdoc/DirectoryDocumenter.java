package io.github.leawind.systemstoragelib.v1.api.dirdoc;

import io.github.leawind.systemstoragelib.v1.Factory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;

/// Ensures some directories contain a descriptive introduction file.
///
/// Memorized paths are checked during {@link #createDirectories}; if an introduction file is
/// missing, it is auto-generated.
/// All operations are thread-safe. Paths are normalized to absolute form for consistent matching.
public interface DirectoryDocumenter {

  static DirectoryDocumenter.Mutable mutable(String fileName) {
    return Factory.createMutableDirectoryDocumenter(fileName);
  }

  Map<String, String> properties();

  /// Unmodifiable view of memorized paths and their introductions.
  Map<Path, String> memories();

  /// Checks if a path has been memorized (filesystem state not considered).
  boolean remember(Path path);

  /// Creates a directory and ensures introduction files exist for all memorized ancestors.
  ///
  /// - Creates missing parent directories as needed
  /// - Checks each level in the path: if memorized and introduction missing, creates it
  /// - Existing introduction files are never overwritten
  /// - Only the target directory receives the provided `attrs`
  /// - Placeholder strings of the form `${name}` in the introduction are replaced with values
  ///   from {@link Mutable#properties()}.
  /// - If a property is not found, it is left as is.
  void createDirectories(Path path, FileAttribute<?>... attrs) throws IOException;

  /// Checks a directory and its ancestors for introduction files.
  /// If any introduction files are missing, they are created.
  ///
  /// Ignores any nonexistent directories or existing introduction files
  void patrol(Path path) throws IOException;

  interface Mutable extends DirectoryDocumenter {
    Map<String, String> properties();

    /// Memorizes a directory path with its introduction for later auto-generation.
    Mutable memorize(Path path, String introduction);

    /// Removes a path from the memorized set.
    void forget(Path path);

    /// Clears all memorized paths.
    void forgetAll();

    default Mutable memorizeByResource(Path path, String name) {
      Class<?> clazz =
          StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();

      try (InputStream inputStream = clazz.getResourceAsStream(name)) {
        if (inputStream == null) {
          throw new NullPointerException("Resource not found: " + name);
        }
        memorize(path, new String(inputStream.readAllBytes()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    default Mutable extendFrom(DirectoryDocumenter parent) {
      properties().putAll(parent.properties());
      parent.memories().forEach(this::memorize);
      return this;
    }
  }
}
