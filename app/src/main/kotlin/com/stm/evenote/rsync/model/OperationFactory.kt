package com.stm.evenote.rsync.model

interface OperationFactory {
    fun noOp(file: SyncFile): Operation
    fun updateFile(fileToUpdate: SyncFile, fileWithNewContent: SyncFile): Operation
    fun delete(fileToDelete: SyncFile): Operation
    fun newFile(newFile: SyncFile): Operation
}