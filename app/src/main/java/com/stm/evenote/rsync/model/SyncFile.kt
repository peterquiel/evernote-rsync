package com.stm.evenote.rsync.model

import org.apache.commons.codec.binary.Hex
import java.util.*

class SyncFile {

    var filename: SyncPath = SyncPath.EMPTY
    var timestamp: Long = 0
    var md5Hash: String = ""
    var dataSupplier: () -> ByteArray = { ByteArray(0) }

    fun matchesHash(other: SyncFile): Boolean {
        return Objects.equals(this.md5Hash, other.md5Hash)
    }

    fun equalsPath(otherFile: SyncFile): Boolean {
        return Objects.equals(this.filename, otherFile.filename)
    }

    fun matches(pattern: String): Boolean {
        return this.filename.matches(pattern)
    }

    fun withSyncPath(filename: SyncPath): SyncFile {
        this.filename = filename
        return this
    }

    fun withPath(path: String): SyncFile {

        this.filename = SyncPath.EMPTY.append(path)
        return this
    }

    fun withDataSupplier(dataSupplier: () -> ByteArray): SyncFile {
        this.dataSupplier = dataSupplier
        return this
    }

    fun withHash(hash: String): SyncFile {

        this.md5Hash = hash
        return this
    }

    fun withTimestamp(timestamp: Long): SyncFile {
        this.timestamp = timestamp
        return this
    }

    fun withHash(hash: ByteArray): SyncFile {

        this.md5Hash = Hex.encodeHexString(hash)
        return this
    }

    fun getData(): ByteArray {
        return dataSupplier.invoke()
    }

    override fun toString(): String {
        return this.filename.toString() + " (MD5:" + this.md5Hash + ")"
    }
}