package io.github.leawind.systemstoragelib.v1.impl.dirdoc;

import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class MutableDirectoryDocumenterImpl implements DirectoryDocumenter.Mutable {
  private static final Pattern PROPERTY_PATTERN =
      Pattern.compile("\\$\\{\\s*([a-zA-Z0-9_\\-]+)\\s*}");

  private final String fileName;
  private final ConcurrentHashMap<Path, String> introductions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();

  public MutableDirectoryDocumenterImpl(String fileName) {
    this.fileName = Objects.requireNonNull(fileName, "fileName cannot be null");
  }

  @Override
  public Map<String, String> properties() {
    return properties;
  }

  @Override
  public Map<Path, String> memories() {
    return Collections.unmodifiableMap(introductions);
  }

  @Override
  public boolean remember(Path path) {
    Objects.requireNonNull(path, "path cannot be null");
    return introductions.containsKey(normalizePath(path));
  }

  @Override
  public Mutable memorize(Path path, String introduction) {
    Objects.requireNonNull(path, "path cannot be null");
    Objects.requireNonNull(introduction, "introduction cannot be null");
    introductions.put(normalizePath(path), introduction);
    return this;
  }

  @Override
  public void forget(Path path) {
    Objects.requireNonNull(path, "path cannot be null");
    introductions.remove(normalizePath(path));
  }

  @Override
  public void forgetAll() {
    introductions.clear();
  }

  @Override
  public void createDirectories(Path path, FileAttribute<?>... attrs) throws IOException {
    Objects.requireNonNull(path, "path cannot be null");

    path = normalizePath(path);

    // order: from root to leaf, all normalized
    List<Path> paths = new ArrayList<>();
    Path current = path;
    while (current != null) {
      paths.add(0, current);
      current = current.getParent();
    }

    for (var level : paths) {
      if (Files.notExists(level)) {
        if (level.equals(path)) {
          // leaf
          Files.createDirectory(level, attrs);
        } else {
          Files.createDirectory(level);
        }
      }

      String introduction = introductions.get(level);

      if (introduction == null) {
        continue;
      }

      Path filePath = level.resolve(fileName);

      if (Files.exists(filePath)) {
        continue;
      }

      Files.writeString(filePath, applyProperties(introduction), StandardOpenOption.CREATE_NEW);
    }
  }

  @Override
  public void patrol(Path path) throws IOException {
    Objects.requireNonNull(path, "path cannot be null");

    path = normalizePath(path);

    // iterate from leaf to root
    for (Path current = path; current != null; current = current.getParent()) {
      if (Files.notExists(current)) {
        continue;
      }

      String introduction = introductions.get(current);
      if (introduction == null) {
        continue;
      }

      Path filePath = current.resolve(fileName);
      if (Files.exists(filePath)) {
        continue;
      }

      Files.writeString(filePath, applyProperties(introduction), StandardOpenOption.CREATE_NEW);
    }
  }

  private String applyProperties(String introduction) {
    return PROPERTY_PATTERN
        .matcher(introduction)
        .replaceAll((m) -> properties().getOrDefault(m.group(1), m.toString()));
  }

  private Path normalizePath(Path path) {
    return path.toAbsolutePath().normalize();
  }
}
