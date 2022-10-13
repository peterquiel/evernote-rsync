package com.stm.evenote.rsync.model;

import java.io.File;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.codec.binary.Hex;

public class SyncFile {

  private SyncPath filename;

  /**
   * MD5 hash alg. is used by evernote as well
   * So we can use it for quick change detection
   * to save bandwidth.
   */
  private String md5Hash;

  private Supplier<byte[]> dataSupplier;

  private long timestamp;

  public boolean matchesHash(SyncFile other) {

    return Objects.equals(this.md5Hash, other.md5Hash);
  }

  public boolean matchesPath(SyncFile other) {

    return Objects.equals(this.filename, other.filename);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SyncPath getFilename() {

    return filename;
  }

  public SyncFile withSyncPath(SyncPath filename) {

    this.filename = filename;
    return this;
  }

  public SyncFile withPath(String path) {

    this.filename = SyncPath.EMPTY.append(path);
    return this;
  }

  public Supplier<byte[]> getDataSupplier() {

    return dataSupplier;
  }

  public SyncFile withDataSupplier(Supplier<byte[]> dataSupplier) {

    this.dataSupplier = dataSupplier;
    return this;
  }

  public String getMd5Hash() {

    return md5Hash;
  }

  public SyncFile withHash(String hash) {

    this.md5Hash = hash;
    return this;
  }

  public SyncFile withHash(byte[] hash) {

    this.md5Hash = Hex.encodeHexString(hash);
    return this;
  }

  public SyncFile withTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public File toFile(String workingDirectory) {

    return new File(this.filename.prepend(workingDirectory).toString());
  }

  @Override
  public String toString() {

    return this.filename + " (MD5:" + this.md5Hash + ")";
  }
}
