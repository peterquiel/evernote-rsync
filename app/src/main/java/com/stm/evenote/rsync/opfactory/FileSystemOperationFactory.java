package com.stm.evenote.rsync.opfactory;

import java.io.File;
import java.io.IOException;

import com.stm.evenote.rsync.model.Operation;
import com.stm.evenote.rsync.model.OperationFactory;
import com.stm.evenote.rsync.model.SyncFile;
import com.stm.evenote.rsync.model.SyncPath;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemOperationFactory implements OperationFactory {

  private static final Logger logger = LoggerFactory.getLogger(FileSystemOperationFactory.class);

  private final String workingDirectory;
  private final Boolean delete;

  public FileSystemOperationFactory(String workingDirectory, Boolean delete) {

    this.workingDirectory = workingDirectory;
    this.delete = delete;
  }

  @Override
  public Operation noOp(SyncFile file) {

    return () -> logger.debug("No operation for file {}", file);
  }

  @Override
  public Operation updateFile(SyncFile fileToUpdate, SyncFile fileWithNewContent) {

    return () -> {
      var absoluteSyncPath = fileToUpdate.getFilename().prepend(this.workingDirectory);
      logger.info("Updating file '{}' with new data", absoluteSyncPath);
      try {
        final File file = absoluteSyncPath.toFile();
        FileUtils.writeByteArrayToFile(file, fileWithNewContent.getDataSupplier().get());
        file.setLastModified(fileWithNewContent.getTimestamp());
      } catch (IOException e) {
        logger.error("Could not update file '{}' with new data", absoluteSyncPath, e);
      }
    };
  }

  @Override
  public Operation delete(SyncFile syncPathToDelete) {

    return () -> {
      var absoluteFile = syncPathToDelete.getFilename().prepend(this.workingDirectory);
      if (this.delete) {
        if (absoluteFile.isFile()) {
          logger.info("Deleting file '{}'", absoluteFile);
          FileUtils.deleteQuietly(absoluteFile.toFile());
        } else if (absoluteFile.isEmptyDir()) {
          logger.info("Deleting empty directory '{}'", absoluteFile);
          FileUtils.deleteQuietly(absoluteFile.toFile());
        }
      } else {
        logger.info(
          "Deletion inactive. Use 'delete' option to delete file '{}'.",
          syncPathToDelete
        );
      }
    };
  }

  @Override
  public Operation newFile(SyncFile newFile) {

    return () -> {
      var absoluteSyncPath = newFile.getFilename().prepend(this.workingDirectory);
      logger.info("Creating new file '{}'", absoluteSyncPath);
      try {
        final File file = absoluteSyncPath.toFile();
        FileUtils.writeByteArrayToFile(file, newFile.getDataSupplier().get());
        file.setLastModified(newFile.getTimestamp());
      } catch (IOException e) {
        logger.error("Could not create new file {}", newFile, e);
      }
    };
  }
}