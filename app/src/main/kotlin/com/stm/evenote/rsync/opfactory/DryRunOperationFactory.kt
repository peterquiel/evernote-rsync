package com.stm.evenote.rsync.opfactory

import com.stm.evenote.rsync.model.OperationFactory
import com.stm.evenote.rsync.model.SyncFile
import com.stm.evenote.rsync.model.SyncPath
import org.slf4j.LoggerFactory
import java.io.IOException

class DryRunOperationFactory(workingDirectory: String, delete: Boolean, excludeDeleteRegExps: List<String>) :
    AbstractOperationFactory(
        workingDirectory, delete, excludeDeleteRegExps
    ), OperationFactory {

    private val logger = LoggerFactory.getLogger(DryRunOperationFactory::class.java)

    override fun execDeleteDir(absoluteFile: SyncPath) {
        logger.info("Dry Run: won't delete directory '${absoluteFile}'.")
    }

    override fun execDeleteFile(absoluteFile: SyncPath) {
        logger.info("Dry Run: won't file directory '${absoluteFile}'.")
    }

    @Throws(Exception::class)
    override fun execNewFile(newFile: SyncFile, absoluteSyncPath: SyncPath) {
        logger.info("Dry Run: won't create new file '${newFile}'.")
    }

    @Throws(IOException::class)
    override fun execUpdateFile(fileWithNewContent: SyncFile, absoluteSyncPath: SyncPath) {
        logger.info("Dry Run: won't update file '${fileWithNewContent}'.")
    }

}