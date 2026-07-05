<div align="center">
<img src="src/main/resources/logo.128x.png" alt="System Storage Lib" style="image-rendering:pixelated;height:6em;">

# System Storage Lib

一个 Minecraft 库模组，提供遵循操作系统数据目录规范的系统级持久化存储。

A Minecraft library mod providing system-level persistent storage following OS data directory conventions.

## [文档](https://leawind.github.io/zh_cn/SystemStorageLib/) | [Documentation](https://leawind.github.io/en_us/SystemStorageLib/)

</div>

```java
Path data = SystemStorageLib.getInstance() // Get singleton
    .scope("example_mod")                  // Your scope, better with mod id
    .directory(StoreType.DATA);            // get directory path of the store type
```

<div align="center">

![Mod version](https://img.shields.io/github/v/tag/Leawind/SystemStorageLib?label=Mod&color=818181)
![Minecraft version](https://img.shields.io/modrinth/game-versions/t07y8PBv?label=Minecraft&color=108e10)

[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1556147?style=flat&logo=curseforge&color=F1643%5E&cacheSeconds=3600&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/system-storage-lib)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/t07y8PBv?style=flat&logo=modrinth&color=17B85A&cacheSeconds=3600&label=Modrinth)](https://modrinth.com/mod/system-storage-lib)

![GitHub License](https://img.shields.io/github/license/Leawind/SystemStorageLib)

</div>
