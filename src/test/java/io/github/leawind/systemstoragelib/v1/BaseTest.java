package io.github.leawind.systemstoragelib.v1;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

  protected FileSystem fs;
  protected Path tempDir;

  protected SystemStorageLib lib;
  protected Scope scope;

  @BeforeEach
  void setup() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.unix());

    tempDir = fs.getPath("/tmp");
    Files.createDirectories(tempDir);

    lib =
        SystemStorageLib.builder(fs.getPath("/meta_config"))
            .logsDir(fs.getPath("/logs"))
            .storeDir(StoreType.CREDENTIALS, fs.getPath("/credentials"))
            .storeDir(StoreType.CACHE, fs.getPath("/cache"))
            .storeDir(StoreType.CONFIG, fs.getPath("/config"))
            .storeDir(StoreType.DATA, fs.getPath("/data"))
            .storeDir(StoreType.DATA_LOCAL, fs.getPath("/data_local"))
            .build();

    scope = lib.scope("test");
  }

  @AfterEach
  void close() throws IOException {
    fs.close();
  }
}
