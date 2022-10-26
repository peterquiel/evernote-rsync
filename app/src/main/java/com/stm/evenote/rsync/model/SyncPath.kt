package com.stm.evenote.rsync.model

import java.io.File

class SyncPath internal constructor(private val parentPath: SyncPath?, private val segmentName: String?) {
    fun prepend(parentPath: String?): SyncPath {
        return if (parentPath == null || parentPath.trim { it <= ' ' }.isEmpty()) {
            this
        } else {
            SyncPath(null, removeEndingSlash(parentPath)).append(this.toString())
        }
    }

    fun append(childPath: String?): SyncPath {
        return if (childPath == null || childPath.trim { it <= ' ' }.isEmpty()) {
            this
        } else SyncPath(
            this,
            removeStartingSlash(removeEndingSlash(childPath))
        )
    }

    override fun hashCode(): Int {
        var result = segmentName?.hashCode() ?: 0
        result = 31 * result + (parentPath?.hashCode() ?: 0)
        return result
    }

    fun toFile(): File {
        return File(this.toString())
    }

    val isFile: Boolean
        get() {
            val file = toFile()
            return file.exists() && file.isFile
        }

    val isEmptyDir: Boolean
        get() {
            val file = toFile()
            return file.exists() && file.isDirectory && (file.list() == null || file.list()?.isEmpty() ?: true)
        }

    fun matches(pattern: String?): Boolean {
        if (pattern == null) {
            return false
        }
        return this.toString().matches(Regex(pattern))
    }

    override fun equals(other: Any?): Boolean {
        return if (other == null || javaClass != other.javaClass) {
            false
        } else {
            this.toString() == other.toString()
        }
    }

    override fun toString(): String {
        if (parentPath == null) {
            return segmentName ?: ""
        }
        var path = parentPath.toString()
        if (segmentName != null && segmentName.trim { it <= ' ' }.length > 1) {
            if (path.trim { it <= ' ' }.isNotEmpty()) {
                path += pathSeparator
            }
            path += segmentName
        }
        return path
    }

    companion object {
        @JvmField
        val EMPTY = SyncPath(null, null)

        private val pathSeparator = System.getProperty("file.separator")

        private fun removeEndingSlash(segmentName: String?): String? {
            return if (segmentName != null && segmentName.endsWith("/") || segmentName!!.endsWith(pathSeparator)) {
                segmentName.substring(0, segmentName.length - 1)
            } else segmentName
        }

        private fun removeStartingSlash(segmentName: String?): String? {
            return if (segmentName != null && segmentName.startsWith("/") || segmentName!!.startsWith(pathSeparator)) {
                segmentName.substring(1)
            } else segmentName
        }
    }
}