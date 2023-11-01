package com.stm.evenote.rsync.model

import com.evernote.clients.NoteStoreClient
import com.evernote.edam.notestore.NoteFilter
import com.evernote.edam.type.Note
import com.evernote.edam.type.Notebook
import com.evernote.edam.type.Resource
import org.slf4j.LoggerFactory
import java.util.stream.Stream

class EvernoteSyncFileFactory(private val noteStoreClient: NoteStoreClient) {

    private val log = LoggerFactory.getLogger(EvernoteSyncFileFactory::class.java)
    private val notebooks: MutableList<Notebook> = ArrayList()

    fun loadAllNotebooks(): EvernoteSyncFileFactory {
        return try {
            log.info("Loading evernote notebook list")
            notebooks.clear()
            notebooks.addAll(noteStoreClient.listNotebooks())
            log.info("Loaded ${notebooks.size} evernote notebooks.")
            this
        } catch (e: Exception) {
            throw RuntimeException("Could not load evernote notebooks.", e)
        }
    }

    fun createForStacks(stacknames: Collection<String?>?): SyncFiles {
        return if (stacknames == null) {
            SyncFiles()
        } else stacknames.filterNotNull().stream()
            .map(::createForStack)
            .reduce(SyncFiles(), SyncFiles::merge)
    }

    fun createForNotebooks(notebooksNames: Collection<String?>?): SyncFiles {
        return if (notebooksNames == null) {
            SyncFiles()
        } else notebooksNames.filterNotNull().stream()
            .map(this::createForNotebook)
            .reduce(SyncFiles(), SyncFiles::merge)
    }

    fun createForStack(stackname: String): SyncFiles {
        return notebookStream()
            .filter { stackname == it.stack }
            .peek { log.info("Found notebook '${it.name}' in stack '${stackname}'") }
            .map(::createForNotebook)
            .reduce(SyncFiles(), SyncFiles::merge)
    }

    fun createForNotebook(notebookName: String): SyncFiles {
        return notebookStream().filter { n: Notebook -> n.name == notebookName }
            .map(::createForNotebook)
            .reduce(SyncFiles()) { obj: SyncFiles, other: SyncFiles -> obj.merge(other) }
    }

    fun createForNotebook(notebook: Notebook): SyncFiles {
        val notes = loadNotesIn(notebook, 0)
        if (notes.isNotEmpty()) {
            log.debug("Found ${notes.size} note[s] in notebook '${notebook.name}'. Processing notes")
            return notes.stream()
                .map { createForNote(it, pathFor(notebook)) }
                .reduce(SyncFiles(), SyncFiles::merge)
        } else {
            log.debug("Notebook '{}' is empty.", notebook.name)
        }
        return SyncFiles()
    }

    private fun notebookStream(): Stream<Notebook> {
        if (notebooks.isEmpty()) {
            log.warn("Notebooks collection is empty. No notebooks found...")
        }
        return notebooks.stream()
    }

    private fun createForNote(note: Note, path: SyncPath): SyncFiles {
        if (note.resources != null && note.resources.isNotEmpty()) {
            val syncFiles = SyncFiles()
            var title = replaceSlash(note.title)
            if (note.resources.size == 1) {
                log.info("Note '${note.title}' with guid ${note.guid} has exactly one resources; Using note title as resources title.")
                note.resources.filterNotNull().stream()
                    .map { createForResource(it, path.append(title + getFileSuffix(it))) }
                    .peek { it.withTimestamp(note.updated) }
                    .forEach(syncFiles::add)
            } else {
                log.info(
                    "Note '${note.title}' with guid ${note.guid} has ${note.resources.size} resources; processing resources"
                )
                note.resources.filterNotNull().stream()
                    .map { createForResource(it, path.append(title).append(it.attributes.fileName)) }
                    .peek { it.withTimestamp(note.updated) }
                    .forEach(syncFiles::add)
            }
            return syncFiles
        } else {
            log.info("Note '${note.title}' with guid ${note.guid} has no resources; Skipping.")
        }
        return SyncFiles()
    }

    private fun getFileSuffix(r: Resource): String {
        val fileName = r.attributes.fileName
        if (fileName != null) {
            val beginIndex = fileName.lastIndexOf(".")
            if (beginIndex > 0) {
                return fileName.substring(beginIndex)
            }
        }
        return ""
    }

    fun createForResource(resource: Resource, syncPath: SyncPath): SyncFile {
        return SyncFile()
            .withSyncPath(syncPath)
            .withHash(resource.data.bodyHash)
            .withDataSupplier { loadBinaryDataFrom(resource) }
    }

    private fun loadBinaryDataFrom(resource: Resource): ByteArray {
        return try {
            noteStoreClient.getResource(
                resource.guid, true,
                false, false, false
            ).data.body
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun pathFor(notebook: Notebook): SyncPath {
        return SyncPath.EMPTY.append(replaceSlash(notebook.stack)).append(replaceSlash(notebook.name))
    }

    private fun replaceSlash(path: String): String {
        return path.replace("/", "_")
    }


    private fun loadNotesIn(notebook: Notebook, offset: Int): List<Note> {
        val noteFilter = NoteFilter()
        noteFilter.notebookGuid = notebook.guid
        return try {
            val maxNotes = 100
            val notes = noteStoreClient.findNotes(noteFilter, offset, maxNotes)
            val noteList = ArrayList<Note>()
            if (notes.notesSize > 0) {
                noteList.addAll(notes.notes)
            }
            if (notes.notesSize >= maxNotes) {
                noteList.addAll(loadNotesIn(notebook, offset + notes.notesSize))
            }
            noteList
        } catch (e: Exception) {
            throw RuntimeException("Could not load notes for notebook '${notebook.name}' with guid ${notebook.guid}", e)
        }
    }
}