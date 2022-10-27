package com.stm.evenote.rsync.model

import java.util.*
import java.util.stream.Collectors

class SyncFiles {

    private val syncFiles: MutableList<SyncFile> = ArrayList()

    constructor()
    constructor(files: Collection<SyncFile>?) {
        syncFiles.addAll(files!!)
    }

    fun add(syncFile: SyncFile): SyncFiles {
        syncFiles.add(syncFile)
        return this
    }

    fun getSyncFiles(): Collection<SyncFile> {
        return Collections.unmodifiableList(syncFiles)
    }

    fun merge(other: SyncFiles): SyncFiles {
        val syncFiles = SyncFiles(syncFiles)
        syncFiles.syncFiles.addAll(other.syncFiles)
        return syncFiles
    }

    fun migrateTo(newState: SyncFiles, operationFactory: OperationFactory): List<Operation> {
        val createOperation = newState.syncFiles.stream()
            .map { f -> migrateTo(f, operationFactory) }
            .toList()
        val deleteOperations = syncFiles.stream()
            .filter { f -> newState.findByPath(f).isEmpty }
            .map { fileToDelete -> operationFactory.delete(fileToDelete) }
            .toList()
        val operations = ArrayList(createOperation)
        operations.addAll(deleteOperations)
        return operations.filterNotNull()
    }

    private fun migrateTo(newState: SyncFile, operationFactory: OperationFactory): Operation {
        val samePath = findByPath(newState)
        val sameHash = findAllByHash(newState)

        return if (samePath.isPresent) {
            sameHash.findByPath(newState)
                .map { operationFactory.noOp(newState) }
                .orElse(operationFactory.updateFile(samePath.get(), newState))
        } else {
            operationFactory.newFile(newState)
        }
    }

    private fun findByPath(other: SyncFile): Optional<SyncFile> {
        return syncFiles.stream()
            .filter { f -> f.equalsPath(other) }
            .findFirst()
    }

    private fun findAllByHash(other: SyncFile): SyncFiles {
        val collect = syncFiles.stream()
            .filter { f -> f.matchesHash(other) }
            .collect(Collectors.toList())
        return SyncFiles(collect)
    }

    override fun toString(): String {
        return syncFiles.stream().map(SyncFile::toString).collect(Collectors.joining("\n"))
    }
}