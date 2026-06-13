<div align="center">
<img src="src/main/resources/logo.128x.png" alt="System Storage Lib" style="image-rendering:pixelated;height:6em;">

# System Storage Lib

[中文](README.zh.md) | [English](README.md) | Русский

Библиотека-мод для Minecraft, предоставляющая другим модам системное持久化хранилище, следующее стандартам каталогов данных каждой операционной системы.

</div>

- **Стандартизированное расположение** — соответствие соглашениям XDG/Windows/macOS через [directories-jvm](https://github.com/dirs-dev/directories-jvm)
- **Пять типов хранилищ** — кэш, конфигурация, секреты, данные, локальные данные
- **Межпроцессные блокировки** — файловые блокировки чтения/записи между процессами
- **Шифрованное хранилище ключ-значение** — AES-256-GCM с выводом ключа PBKDF2, привязанное к текущей системе

## Быстрый старт

### Добавление зависимости

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
> Технически вы можете включить этот мод в JAR вашего мода и переместить (relocate), но это не рекомендуется.

`fabric.mod.json`:

```json
{
  "depends": {
	  "system-storage-lib": ">=${system_storage_lib_version}"
  }
}
```

`neoforge.mods.toml` / `mods.toml`:

```toml
[[dependencies.example_mod]]
modId = "system-storage-lib"
mandatory=true
versionRange="[${system_storage_lib_version},)"
ordering="NONE"
side="SERVER" # CLIENT / SERVER / BOTH
```

### Пример

```java
Scope scope = SystemStorageLib.getInstance().scope("example_mod");
```

Получение каталога данных:

```java
Path dataDir = scope.directory(StoreType.DATA);
```

Сохранение и чтение зашифрованных секретов:

```java
Scope scope = SystemStorageLib.getInstance().scope("example_mod");

var secrets = scope.access(StoreType.SECRETS, SecretsAccessor::from);
secrets.set("some_token","secret_value_123");

String token = secrets.get("some_token"); // "secret_value_123"
```

> [!Warning]
>
> Шифрование учётных данных защищает только от случайной утечки через скриншоты, общий доступ к экрану, облачную синхронизацию и т.п., но не защищает от вредоносных программ, работающих на локальной системе.

## Типы хранилищ

| Значение `StoreType` | Тип хранилища    | Описание                                                                                      |
| -------------------- | ---------------- | --------------------------------------------------------------------------------------------- |
| `CACHE`              | Кэш              | Восстанавливаемые данные                                                                      |
| `CONFIG`             | Конфигурация     | Файлы настроек, например, пользовательские предпочтения                                       |
| `SECRETS`            | Секреты          | Чувствительные данные, требующие шифрования (токены, ключи и т.д.)                            |
| `DATA`               | Данные           | Постоянные данные, которые могут быть общими для нескольких машин                             |
| `DATA_LOCAL`         | Локальные данные | Постоянные данные, специфичные для текущей машины, или кэш-данные, дорогие для восстановления |
