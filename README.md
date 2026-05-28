<div align="center">
<img src="src/main/resources/logo.128x.png" alt="System Storage Lib" style="image-rendering:pixelated;height:6em;">

# System Storage Lib

[中文](README.zh.md) | English | [Русский](README.ru.md)

A Minecraft library mod providing system-level persistent storage for other mods, following data directory conventions of each operating system.

</div>

- **Standard Storage Locations** — Follows XDG/Windows/macOS conventions via [directories-jvm](https://github.com/dirs-dev/directories-jvm)
- **Five Storage Types** — Cache, Config, Credentials, Data, Local Data
- **Cross-Process Locking** — File-based cross-process read/write locks
- **Encrypted Key-Value Storage** — AES-256-GCM with PBKDF2 key derivation, bound to the current system

## Quick Start

### Adding Dependency

`gradle.properties`:

```properties
system_storage_lib_version=0.1.0
```

`build.gradle.kts` (Kotlin):

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    modImplementation("com.github.Leawind:SystemStorageLib:${project.property("system_storage_lib_version")}")
}
```

> [!Tip]
>
> Technically, you can include this mod in your mod jar and relocate it, but this is not recommended.

`fabric.mod.json`:

```json
{
  "depends": {
    "system_storage_lib": ">=${system_storage_lib_version}"
  }
}
```

`neoforge.mods.toml` / `mods.toml`:

```toml
[[dependencies.example_mod]]
modId="system_storage_lib"
mandatory=true
versionRange="[${system_storage_lib_version},)"
ordering="NONE"
side="SERVER" # CLIENT / SERVER / BOTH
```

### Examples

Get a scope:

```java
Scope scope = SystemStorageLib.getInstance().scope("example_mod");
```

Get the data directory:

```java
Storage dataStorage = scope.storage(StoreType.DATA);
Path dataDir = dataStorage.getDirPath();
```

Store and retrieve encrypted credentials:

```java
Scope scope = SystemStorageLib.getInstance().scope("example_mod");

CredentialStore credentials = scope.storage(StoreType.CREDENTIALS).map(CredentialStore::of);
credentials.set("some_token", "secret_value_123");
String token = credentials.get("some_token"); // "secret_value_123"
```

> [!Warning]
>
> Credential encryption only protects against accidental leaks such as screenshots, screen sharing, and cloud sync. It cannot protect against malicious programs running locally.

## Storage Types

| `StoreType` Enum Value | Storage Type | Description                                                                                    |
| ---------------------- | ------------ | ---------------------------------------------------------------------------------------------- |
| `CACHE`                | Cache        | Regenerable data                                                                               |
| `CONFIG`               | Config       | Configuration files, such as user preferences                                                  |
| `CREDENTIALS`          | Credentials  | Sensitive data requiring encryption (tokens, keys, etc.)                                       |
| `DATA`                 | Data         | Persistent data that can be shared across machines                                             |
| `DATA_LOCAL`           | Local Data   | Persistent data specific to the current machine, or cache data that is expensive to regenerate |
