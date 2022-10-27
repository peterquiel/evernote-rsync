package com.stm.evenote.rsync.opfactory

import com.stm.evenote.rsync.model.OperationFactory
import com.stm.evenote.rsync.model.SyncFile
import com.stm.evenote.rsync.model.SyncPath
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit

class FileSystemOperationFactory(
    workingDirectory: String,
    delete: Boolean,
    excludeDeleteRegExps: List<String>
) :

    AbstractOperationFactory(
        workingDirectory, delete, excludeDeleteRegExps
    ), OperationFactory {

    private val logger = LoggerFactory.getLogger(FileSystemOperationFactory::class.java)

    override fun execDeleteDir(absoluteFile: SyncPath) {
        logger.info("Deleting empty directory '${absoluteFile}'")
        val path = Paths.get(absoluteFile.toString())
        try {
            Files.delete(path)
        } catch (e: IOException) {
            logger.error("Could not delete directory '${absoluteFile}'", e)
        }
    }

    override fun execDeleteFile(absoluteFile: SyncPath) {
        logger.info("Deleting file '{}'", absoluteFile)
        val path = Paths.get(absoluteFile.toString())
        try {
            Files.delete(path)
        } catch (e: IOException) {
            logger.error("Could not delete file '${absoluteFile}'", e)
        }
    }

    override fun execNewFile(newFile: SyncFile, absoluteSyncPath: SyncPath) {
        execUpdateFile(newFile, absoluteSyncPath)
    }

    override fun execUpdateFile(fileWithNewContent: SyncFile, absoluteSyncPath: SyncPath) {
        logger.info("Creating/updating file '${absoluteSyncPath}' with new data")
        val path = Paths.get(absoluteSyncPath.toString())
        try {
            Files.createDirectories(path.parent)
            Files.write(path, fileWithNewContent.getData(), StandardOpenOption.CREATE)
            Files.setLastModifiedTime(path, FileTime.from(fileWithNewContent.timestamp, TimeUnit.SECONDS))
        } catch (ie: IOException) {
            throw RuntimeException("Could not update file '${absoluteSyncPath}'", ie)
        }
    }

}