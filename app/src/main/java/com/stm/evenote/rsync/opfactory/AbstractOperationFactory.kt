package com.stm.evenote.rsync.opfactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.stm.evenote.rsync.model.Operation;
import com.stm.evenote.rsync.model.OperationFactory;
import com.stm.evenote.rsync.model.SyncFile;
import com.stm.evenote.rsync.model.SyncPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractOperationFactory implements OperationFactory {

  private static final Logger logger = LoggerFactory.getLogger(AbstractOperationFactory.class);

  protected final String workingDirectory;
  protected final Boolean delete;
  protected final List<String> excludeDeletionRegExps;

  AbstractOperationFactory(String workingDirectory, Boolean delete, List<String> excludeDeleteRegExps) {
    this.workingDirectory = workingDirectory;
    this.delete = delete;
    this.excludeDeletionRegExps = excludeDeleteRegExps == null ? Collections.emptyList() : excludeDeleteRegExps;
  }

  public Operation noOp(SyncFile file) {

    return () -> logger.debug("No operation for file '{}'", absolutePath(file));
  }

  public Operation updateFile(SyncFile fileToUpdate, SyncFile fileWithNewContent) {

    return () -> {
      var absoluteSyncPath = absolutePath(fileToUpdate);
      try {
        execUpdateFile(fileWithNewContent, absoluteSyncPath);
      } catch (IOException e) {
        logger.error("Could not update file '{}' with new data", absoluteSyncPath, e);
      }
    };
  }

  public Operation delete(SyncFile syncPathToDelete) {

    return () -> {
      var matchedExcludePattern = this.excludeDeletionRegExps.stream()
        .filter(syncPathToDelete::matches)
        .findFirst();

      var absoluteFile = absolutePath(syncPathToDelete);
      if (this.delete) {
        if (absoluteFile.isFile()) {
          if (matchedExcludePattern.isEmpty()) {
            execDeleteFile(absoluteFile);
          } else {
            logger.info("File '{}' matched exclude pattern '{}'. Won't delete.",
              absoluteFile, matchedExcludePattern.get());
          }
        } else if (absoluteFile.isEmptyDir()) {
          if (matchedExcludePattern.isEmpty()) {
            execDeleteDir(absoluteFile);
          } else {
            logger.info("Empty directory '{}' matches '{}'. Won't delete.",
              absoluteFile, matchedExcludePattern.get());
          }
        }
      } else {
        logger.info(
          "Deletion inactive. Use 'delete' option to delete file '{}'.",
          absoluteFile
        );
      }
    };
  }

  public Operation newFile(SyncFile newFile) {

    return () -> {
      var absoluteSyncPath = absolutePath(newFile);
      try {
        execNewFile(newFile, absoluteSyncPath);
      } catch (Exception e) {
        logger.error("Could not create new file '{}'", absoluteSyncPath, e);
      }
    };
  }

  protected abstract void execDeleteDir(SyncPath absoluteFile);

  protected abstract void execDeleteFile(SyncPath absoluteFile);

  protected abstract void execNewFile(SyncFile newFile, SyncPath absoluteSyncPath) throws Exception;

  protected abstract void execUpdateFile(SyncFile fileWithNewContent, SyncPath absoluteSyncPath) throws IOException;

  private SyncPath absolutePath(SyncFile fileToUpdate) {
    return fileToUpdate.getFilename().prepend(this.workingDirectory);
  }
}
