package com.stm.evenote.rsync.opfactory;

import java.io.File;

import com.stm.evenote.rsync.model.Operation;
import com.stm.evenote.rsync.model.OperationFactory;
import com.stm.evenote.rsync.model.SyncFile;
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
  public Operation deleteFile(SyncFile fileToDelete) {

    return () -> {
      final File file = fileToDelete.toFile(this.workingDirectory);
      if (file.exists()) {
        logger.info("Dry Run: file {} exists and would be deleted", fileToDelete);
      } else {
        logger.info("Dry Run: file {} does not exist and should be deleted. Nothing to do.", fileToDelete);

      }
    };
  }

  @Override
  public Operation newFile(SyncFile newFile) {
    return () -> logger.info("Creating new file {}", newFile);
  }

}
