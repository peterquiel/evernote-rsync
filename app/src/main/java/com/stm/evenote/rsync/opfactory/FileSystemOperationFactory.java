package com.stm.evenote.rsync.opfactory;

import java.io.File;
import java.io.IOException;

import com.stm.evenote.rsync.model.Operation;
import com.stm.evenote.rsync.model.OperationFactory;
import com.stm.evenote.rsync.model.SyncFile;
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
      logger.info("Updating file {} with data from {}", fileToUpdate, fileWithNewContent);
      try {
        final File file = fileToUpdate.toFile(this.workingDirectory);
        FileUtils.writeByteArrayToFile(file, fileWithNewContent.getDataSupplier().get());
        file.setLastModified(fileWithNewContent.getTimestamp());
      } catch (IOException e) {
        logger.error("Could update file {} with data from {}", fileToUpdate, fileWithNewContent, e);
      }
    };
  }

  @Override
  public Operation deleteFile(SyncFile fileToDelete) {

    return () -> {
      final File file = fileToDelete.toFile(this.workingDirectory);
      if (file.exists()) {
        if (this.delete) {
          logger.info("Deleting file {}", fileToDelete);
          FileUtils.deleteQuietly(file);
        } else {
          logger.info("File {} not found in Evernote. Use 'delete' option to delete on local drive.", fileToDelete);
        }
      }
    };
  }

  @Override
  public Operation newFile(SyncFile newFile) {

    return () -> {
      logger.info("Creating new file {}", newFile);
      try {
        final File file = newFile.toFile(this.workingDirectory);
        FileUtils.writeByteArrayToFile(file, newFile.getDataSupplier().get());
        file.setLastModified(newFile.getTimestamp());
      } catch (IOException e) {
        logger.error("Could create new file {}", newFile, e);
      }
    };
  }
}