package com.stm.evenote.rsync.opfactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.stm.evenote.rsync.model.OperationFactory;
import com.stm.evenote.rsync.model.SyncFile;
import com.stm.evenote.rsync.model.SyncPath;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemOperationFactory extends AbstractOperationFactory implements OperationFactory {

  private static final Logger logger = LoggerFactory.getLogger(FileSystemOperationFactory.class);

  public FileSystemOperationFactory(String workingDirectory, Boolean delete, List<String> excludeDeleteRegExps) {
    super(workingDirectory, delete, excludeDeleteRegExps);
  }

  @Override
  protected void execDeleteDir(SyncPath absoluteFile) {
    logger.info("Deleting empty directory '{}'", absoluteFile);
    var path = Paths.get(absoluteFile.toString());
    try {
      Files.delete(path);
    } catch (IOException e) {
      logger.error("Could not delete directory '{}'", absoluteFile, e);
    }
  }

  @Override
  protected void execDeleteFile(SyncPath absoluteFile) {
    logger.info("Deleting file '{}'", absoluteFile);
    var path = Paths.get(absoluteFile.toString());
    try {
      Files.delete(path);
    } catch (IOException e) {
      logger.error("Could not delete file '{}'", absoluteFile, e);
    }
  }

  @Override
  protected void execNewFile(SyncFile newFile, SyncPath absoluteSyncPath) throws Exception {
    execUpdateFile(newFile, absoluteSyncPath);
  }

  @Override
  protected void execUpdateFile(SyncFile fileWithNewContent, SyncPath absoluteSyncPath) throws IOException {
    logger.info("Creating/updating file '{}' with new data", absoluteSyncPath);
    var path = Paths.get(absoluteSyncPath.toString());
    Files.createDirectories(path.getParent());
    Files.write(path, fileWithNewContent.getDataSupplier().get(), StandardOpenOption.CREATE);
    Files.setLastModifiedTime(path, FileTime.from(fileWithNewContent.getTimestamp(), TimeUnit.SECONDS));
  }
}