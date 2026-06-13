<div align="center">
<img src="src/main/resources/logo.128x.png" alt="System Storage Lib" style="image-rendering:pixelated;height:6em;">

# System Storage Lib

[中文](README.zh.md) | [English](README.md) | Русский

Библиотека-мод для Minecraft, предоставляющая системное постоянное хранилище для других модов в соответствии с
соглашениями о каталогах данных операционных систем.

</div>

- **Стандартное расположение хранилища** — соблюдение соглашений XDG/Windows/macOS
  через [directories-jvm](https://github.com/dirs-dev/directories-jvm)
- **Пять типов хранилищ** — Кэш, Конфигурация, Секреты, Данные, Локальные данные
- **Блокировка между процессами** — основанная на файлах блокировка чтения-записи для межпроцессного взаимодействия
- **Зашифрованное хранилище ключ-значение** — AES-256-GCM с формированием ключа PBKDF2, привязанное к текущей системе

## Быстрый старт

### Подключение зависимости

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
> Технически вы можете включить этот мод в jar вашего мода и выполнить relocate, но это не рекомендуется.

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

Хранение и чтение зашифрованных ключей:

```java
Scope scope = SystemStorageLib.getInstance().scope("example_mod");

var secrets = scope.access(StoreType.SECRETS, SecretsAccessor::from);
secrets.set("some_token","secret_value_123");

String token = secrets.get("some_token"); // "secret_value_123"
```

> [!Warning]
>
> Шифрование учетных данных защищает только от случайной утечки — такой как скриншоты, демонстрация экрана, облачная
> синхронизация — но не от вредоносных программ, запущенных локально.

## Типы хранилищ

| Значение `StoreType` | Тип хранилища    | Описание                                                                                            |
|----------------------|------------------|-----------------------------------------------------------------------------------------------------|
| `CACHE`              | Кэш              | Восстанавливаемые данные                                                                            |
| `CONFIG`             | Конфигурация     | Файлы конфигурации, например, пользовательские настройки                                            |
| `SECRETS`            | Секреты          | Конфиденциальные данные, требующие шифрования (токены, ключи и т.д.)                                |
| `DATA`               | Данные           | Постоянные данные, которые можно передавать между машинами                                          |
| `DATA_LOCAL`         | Локальные данные | Постоянные данные, специфичные для текущей машины, или кэш с высокой стоимостью повторной генерации |
