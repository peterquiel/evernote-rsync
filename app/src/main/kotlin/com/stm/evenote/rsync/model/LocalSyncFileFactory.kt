package com.stm.evenote.rsync.model

import org.apache.commons.io.FileUtils
import org.apache.commons.io.file.PathUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.Path
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class LocalSyncFileFactory {
    fun createFor(directory: File): SyncFiles {
        val syncFiles = SyncFiles()
        if (directory.exists()) {
            try {
                PathUtils.walk(
                    directory.toPath(),
                    TrueFileFilter.TRUE, Int.MAX_VALUE,
                    false, FileVisitOption.FOLLOW_LINKS
                )
                    .map(Path::toFile)
                    .map { f: File -> fromFile(directory, f) }
                    .filter(Objects::nonNull)
                    .forEach(syncFiles::add)
            } catch (e: IOException) {
                logger.error("Could not travers directory '{}'. Check permission.", directory.absoluteFile, e)
            }
        }
        return syncFiles
    }

    fun fromFile(workingDirectory: File, file: File): SyncFile {
        val workingDirPath = workingDirectory.path
        val relativeFilePath = file.path.substring(workingDirPath.length)
        val fileName = SyncPath.EMPTY.append(relativeFilePath)
        return SyncFile()
            .withSyncPath(fileName)
            .withDataSupplier { readFile(file) }
            .withTimestamp(file.lastModified())
            .withHash(md!!.digest(readFile(file)))
    }

    private fun readFile(file: File): ByteArray {
        return try {
            if (file.isFile) {
                FileUtils.readFileToByteArray(file)
            } else {
                ByteArray(0)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(LocalSyncFileFactory::class.java)
        private val md: MessageDigest?

        init {
            try {
                md = MessageDigest.getInstance("MD5")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Could not instantiate MD5 hash ", e)
            }
        }
    }
}