package com.stm.evenote.rsync.model;

public interface OperationFactory {

    Operation noOp(SyncFile file);

    Operation updateFile(SyncFile fileToUpdate, SyncFile fileWithNewContent);

    Operation delete(SyncFile fileToDelete);

    Operation newFile(SyncFile targetFile);
}
