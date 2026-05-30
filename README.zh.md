<div align="center">
<img src="src/main/resources/logo.128x.png" alt="System Storage Lib" style="image-rendering:pixelated;height:6em;">

# System Storage Lib

中文 | [English](README.md) | [Русский](README.ru.md)

一个 Minecraft 库模组，为其他模组提供系统级持久化存储，遵循各操作系统的数据目录规范。

</div>

- **规范存储位置** — 通过 [directories-jvm](https://github.com/dirs-dev/directories-jvm) 遵循 XDG/Windows/macOS 约定
- **五种存储类型** — 缓存、配置、密钥、数据、本地数据
- **跨进程锁** — 基于文件的跨进程读写锁
- **加密键值存储** — AES-256-GCM 配合 PBKDF2 密钥派生，绑定到当前系统

## 快速开始

### 引入依赖

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
> 技术上你可以将本模组包含进你的模组 jar 中并 relocate，但不建议这么做。

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

### 示例

```java
Scope scope = SystemStorageLib.getInstance().scope("example_mod");
```

获取数据目录：

```java
Path dataDir = scope.directory(StoreType.DATA);
```

存储和读取加密密钥：

```java
Scope scope = SystemStorageLib.getInstance().scope("example_mod");

var secrets = scope.access(StoreType.SECRETS, SecretStore::of);
secrets.

set("some_token","secret_value_123");

String token = secrets.get("some_token"); // "secret_value_123"
```

> [!Warning]
>
> 凭据加密仅防范截图、屏幕共享、云同步等意外泄漏，无法防范运行在本地的恶意程序。

## 存储类型

| `StoreType` 枚举值 | 存储类型 | 描述                          |
|-----------------|------|-----------------------------|
| `CACHE`         | 缓存   | 可再生数据                       |
| `CONFIG`        | 配置   | 配置文件，如用户偏好                  |
| `SECRETS`       | 密钥   | 需要加密的敏感数据（令牌、密钥等）           |
| `DATA`          | 数据   | 可跨机器共享的持久化数据                |
| `DATA_LOCAL`    | 本地数据 | 特定于当前机器的持久化数据，或重新生成代价高的缓存数据 |
