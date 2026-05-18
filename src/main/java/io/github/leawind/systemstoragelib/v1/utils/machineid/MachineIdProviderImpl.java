package io.github.leawind.systemstoragelib.v1.utils.machineid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MachineIdProviderImpl {

  private static void bootstrap() {
    MachineIdProviderRegistry registry = MachineIdProviderRegistry.getInstance();

    registry
        .forKeywords("windows")
        .register(
            () -> {
              ProcessBuilder pb =
                  new ProcessBuilder(
                      "reg",
                      "query",
                      "HKLM\\SOFTWARE\\Microsoft\\Cryptography",
                      "/v",
                      "MachineGuid");
              pb.redirectErrorStream(true);
              Process process = null;

              try {
                process = pb.start();

                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                  process.destroyForcibly();
                  throw new IOException("Registry query timed out");
                }

                // reg command output encoding depends on the system active code page
                Charset charset = Charset.defaultCharset();
                try (BufferedReader br =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                  String line;
                  while ((line = br.readLine()) != null) {
                    if (line.contains("MachineGuid")) {
                      String[] parts = line.trim().split("\\s+");
                      if (parts.length > 0) {
                        return parts[parts.length - 1].replace("{", "").replace("}", "");
                      }
                    }
                  }
                }
                throw new IOException("MachineGuid value not found in registry output");
              } catch (InterruptedException e) {
                throw new MachineIdResolutionException("Machine ID retrieval interrupted", e);
              } finally {
                if (process != null) {
                  process.destroy();
                }
              }
            });

    registry
        .forKeywords("linux", "unix")
        .register(
            () -> {
              Path primary = Paths.get("/etc/machine-id");
              Path fallback = Paths.get("/var/lib/dbus/machine-id");

              String id = Files.readString(primary).trim();

              if (id.isEmpty()) {
                id = Files.readString(fallback).trim();
              }

              if (id.isEmpty()) {
                throw new MachineIdResolutionException(
                    "Neither /etc/machine-id nor /var/lib/dbus/machine-id contains a valid identifier");
              }

              return id;
            });

    registry
        .forKeywords("mac", "darwin")
        .register(
            () -> {
              final Pattern MAC_UUID_PATTERN =
                  Pattern.compile("\"IOPlatformUUID\"\\s*=\\s*\"([^\"]+)\"");

              ProcessBuilder pb =
                  new ProcessBuilder("ioreg", "-rd1", "-c", "IOPlatformExpertDevice");
              pb.redirectErrorStream(true);
              Process process = pb.start();

              try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                  process.destroyForcibly();
                  throw new IOException("ioreg command timed out");
                }

                try (BufferedReader br =
                    new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = br.readLine()) != null) {

                    Matcher m = MAC_UUID_PATTERN.matcher(line);
                    if (m.find()) return m.group(1);
                  }
                }
                throw new IOException("IOPlatformUUID not found in ioreg output");
              } catch (InterruptedException e) {
                throw new MachineIdResolutionException("Machine ID retrieval interrupted", e);
              } finally {
                process.destroy();
              }
            });
  }

  static {
    bootstrap();
  }

  public static String resolve() {
    try {
      return MachineIdProviderRegistry.getInstance()
          .getProvider(System.getProperty("os.name", ""))
          .get();
    } catch (IOException e) {
      throw new MachineIdResolutionException("Failed to resolve machine ID", e);
    }
  }
}
