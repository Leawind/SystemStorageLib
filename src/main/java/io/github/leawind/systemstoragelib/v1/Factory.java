package io.github.leawind.systemstoragelib.v1;

import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.accessors.SecretsAccessor;
import io.github.leawind.systemstoragelib.v1.api.dirdoc.DirectoryDocumenter;
import io.github.leawind.systemstoragelib.v1.impl.Holder;
import io.github.leawind.systemstoragelib.v1.impl.SystemStorageLibImpl;
import io.github.leawind.systemstoragelib.v1.impl.accessors.SecretsAccessorImpl;
import io.github.leawind.systemstoragelib.v1.impl.dirdoc.MutableDirectoryDocumenterImpl;
import java.nio.file.Path;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Factory {
  private Factory() {}

  public static DirectoryDocumenter.Mutable createMutableDirectoryDocumenter(String fileName) {
    return new MutableDirectoryDocumenterImpl(fileName);
  }

  public static SecretsAccessor createSecretsAccessor(
      Path dirPath, Scope scope, DirectoryDocumenter directoryDocumenter) {
    return new SecretsAccessorImpl(dirPath, scope, directoryDocumenter);
  }

  public static SystemStorageLib.Builder createLibBuilder(Path metaConfigDir) {
    return new SystemStorageLibImpl.BuilderImpl(metaConfigDir);
  }

  public static SystemStorageLib getSystemStorageLibInstance() {
    return Holder.SYSTEM_STORAGE_LIB;
  }
}
