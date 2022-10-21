package com.stm.evenote.rsync.model

import java.util.*

class SyncFileK(var filename: SyncPath) {

    var timestamp: Long = 0
    var md5Hash: String = ""
    fun matchesHash(other: SyncFileK): Boolean {
        return Objects.equals(this.md5Hash, other.md5Hash)
    }

    fun matchesPath(other: SyncFileK): Boolean {
        return Objects.equals(this.filename, other.filename)
    }

    fun matches(pattern: String): Boolean {
        return this.filename.matches(pattern)
    }

    override fun toString(): String {
        return this.filename.toString() + " (MD5:" + this.md5Hash + ")"
    }


}