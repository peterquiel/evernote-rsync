package com.stm.evenote.rsync.opfactory;

import java.io.IOException;
import java.util.List;

import com.stm.evenote.rsync.model.OperationFactory;
import com.stm.evenote.rsync.model.SyncFile;
import com.stm.evenote.rsync.model.SyncPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DryRunOperationFactory extends AbstractOperationFactory implements OperationFactory {

  private static final Logger logger = LoggerFactory.getLogger(DryRunOperationFactory.class);

  public DryRunOperationFactory(String workingDirectory, Boolean delete, List<String> excludeDeleteRegExps) {
    super(workingDirectory, delete, excludeDeleteRegExps);
  }

  @Override
  protected void execDeleteDir(SyncPath absoluteFile) {
    logger.info("Dry Run: won't delete directory '{}'.", absoluteFile);
  }

  @Override
  protected void execDeleteFile(SyncPath absoluteFile) {
    logger.info("Dry Run: won't file directory '{}'.", absoluteFile);

  }

  @Override
  protected void execNewFile(SyncFile newFile, SyncPath absoluteSyncPath) throws Exception {
    logger.info("Dry Run: won't create new file '{}'.", newFile);

  }

  @Override
  protected void execUpdateFile(SyncFile fileWithNewContent, SyncPath absoluteSyncPath) throws IOException {
    logger.info("Dry Run: won't update file '{}'.", fileWithNewContent);
  }
}
