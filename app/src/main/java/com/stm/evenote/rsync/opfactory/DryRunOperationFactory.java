package com.stm.evenote.rsync.opfactory;

import java.io.File;

import com.stm.evenote.rsync.model.Operation;
import com.stm.evenote.rsync.model.OperationFactory;
import com.stm.evenote.rsync.model.SyncFile;
import com.stm.evenote.rsync.model.SyncPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DryRunOperationFactory implements OperationFactory {

  private static final Logger logger = LoggerFactory.getLogger(DryRunOperationFactory.class);
  private final String workingDirectory;

  public DryRunOperationFactory(String workingDirectory) {

    this.workingDirectory = workingDirectory;
  }

  @Override
  public Operation noOp(SyncFile file) {

    return () -> logger.info("Dry Run: No operation for file {}", file);
  }

  @Override
  public Operation updateFile(SyncFile fileToUpdate, SyncFile srcFile) {

    return () -> logger.info("Dry Run: file {} would be updated with data from {}", fileToUpdate, srcFile);
  }

  @Override
  public Operation delete(SyncFile pathToDelete) {

    return () -> {
      final var file = pathToDelete.getFilename().prepend(this.workingDirectory);
      if (file.isFile()) {
        logger.info("Dry Run: {} exists and would be deleted", pathToDelete);
      } else if(file.isEmptyDir()) {
        logger.info("Dry Run: {} is empty directory and would be deleted", pathToDelete);
      }
    };
  }

  @Override
  public Operation newFile(SyncFile newFile) {
    return () -> logger.info("Creating new file {}", newFile);
  }

}
