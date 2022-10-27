package com.stm.evenote.rsync.opfactory

import com.stm.evenote.rsync.model.Operation
import com.stm.evenote.rsync.model.OperationFactory
import com.stm.evenote.rsync.model.SyncFile
import com.stm.evenote.rsync.model.SyncPath
import org.slf4j.LoggerFactory
import java.io.IOException

abstract class AbstractOperationFactory(
    protected val workingDirectory: String,
    protected val delete: Boolean,
    protected val excludeDeletionRegExps: List<String>
) : OperationFactory {

    private val logger = LoggerFactory.getLogger(AbstractOperationFactory::class.java)

    override fun noOp(file: SyncFile): Operation {
        return Operation { logger.debug("No operation for file '${absolutePath(file)}'") }
    }

    override fun updateFile(fileToUpdate: SyncFile, fileWithNewContent: SyncFile): Operation {
        return Operation {
            val absoluteSyncPath = absolutePath(fileToUpdate)
            try {
                execUpdateFile(fileWithNewContent, absoluteSyncPath)
            } catch (e: IOException) {
                logger.error("Could not update file '${absoluteSyncPath}' with new data", e)
            }
        }
    }

    override fun delete(fileToDelete: SyncFile): Operation {
        return Operation {
            val matchedExcludePattern = excludeDeletionRegExps.stream()
                .filter(fileToDelete::matches)
                .findFirst()
            val absoluteFile = absolutePath(fileToDelete)
            if (delete) {
                if (absoluteFile.isFile) {
                    if (matchedExcludePattern.isEmpty) {
                        execDeleteFile(absoluteFile)
                    } else {
                        logger.info(
                            "File '${absoluteFile}' matched exclude pattern '${matchedExcludePattern.get()}'. Won't delete."
                        )
                    }
                } else if (absoluteFile.isEmptyDir) {
                    if (matchedExcludePattern.isEmpty) {
                        execDeleteDir(absoluteFile)
                    } else {
                        logger.info(
                            "Empty directory '${absoluteFile}' matches '${matchedExcludePattern.get()}'. Won't delete.",
                        )
                    }
                }
            } else {
                logger.info(
                    "Deletion inactive. Use 'delete' option to delete file '${absoluteFile}'.",

                    )
            }
        }
    }

    override fun newFile(newFile: SyncFile): Operation {
        return Operation {
            val absoluteSyncPath = absolutePath(newFile)
            try {
                execNewFile(newFile, absoluteSyncPath)
            } catch (e: Exception) {
                logger.error("Could not create new file '${absoluteSyncPath}'", e)
            }
        }
    }

    protected abstract fun execDeleteDir(absoluteFile: SyncPath)

    protected abstract fun execDeleteFile(absoluteFile: SyncPath)

    protected abstract fun execNewFile(newFile: SyncFile, absoluteSyncPath: SyncPath)

    protected abstract fun execUpdateFile(fileWithNewContent: SyncFile, absoluteSyncPath: SyncPath)

    private fun absolutePath(fileToUpdate: SyncFile): SyncPath {
        return fileToUpdate.filename.prepend(workingDirectory)
    }
}