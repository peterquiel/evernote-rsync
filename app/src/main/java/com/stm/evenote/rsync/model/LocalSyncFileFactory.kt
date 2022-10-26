package com.stm.evenote.rsync.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalSyncFileFactory {

  private static final Logger logger = LoggerFactory.getLogger(LocalSyncFileFactory.class);
  private static final MessageDigest md;

  static {
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Could not instantiate MD5 hash ", e);
    }
  }

  public SyncFiles createFor(File directory) {
    var syncFiles = new SyncFiles();
    if (directory.exists()) {

      try {
        PathUtils.walk(directory.toPath(),
            TrueFileFilter.TRUE, Integer.MAX_VALUE,
            false, FileVisitOption.FOLLOW_LINKS)
          .map(Path::toFile)
          .map(f -> this.fromFile(directory, f))
          .forEach(syncFiles::add);
      } catch (IOException e) {
        logger.error("Could not travers directory '{}'. Check permission.", directory.getAbsoluteFile(), e);
      }
    }
    return syncFiles;
  }

  public SyncFile fromFile(File workingDirectory, File file) {

    var workingDirPath = workingDirectory.getPath();
    var relativeFilePath = file.getPath().substring(workingDirPath.length());
    final SyncPath fileName = SyncPath.EMPTY.append(relativeFilePath);

    return new SyncFile()
      .withSyncPath(fileName)
      .withDataSupplier(() -> readFile(file))
      .withTimestamp(file.lastModified())
      .withHash(md.digest(readFile(file)));
  }

  private byte[] readFile(File file) {

    try {
      if (file.isFile()) {
        return FileUtils.readFileToByteArray(file);
      } else {
        return new byte[0];
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
