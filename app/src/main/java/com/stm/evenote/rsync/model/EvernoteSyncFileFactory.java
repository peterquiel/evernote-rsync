package com.stm.evenote.rsync.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Resource;
import com.evernote.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvernoteSyncFileFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(EvernoteSyncFileFactory.class);

  private final NoteStoreClient noteStoreClient;

  private List<Notebook> notebooks = new ArrayList<>();

  public EvernoteSyncFileFactory(NoteStoreClient noteStoreClient) {

    this.noteStoreClient = noteStoreClient;
  }

  public EvernoteSyncFileFactory loadAllNotebooks() {

    try {
      notebooks = this.noteStoreClient.listNotebooks();
      LOGGER.info("Loaded {} evernote notebooks.", notebooks.size());
      return this;
    } catch (EDAMUserException | EDAMSystemException | TException e) {
      throw new RuntimeException("Could not load evernote notebooks.", e);
    }
  }

  public SyncFiles createForStacks(Collection<String> stackname) {
    if (stackname == null) {
      return new SyncFiles();
    }

    return stackname.stream()
      .map(this::createForStack)
      .reduce(new SyncFiles(), SyncFiles::merge);
  }

  public SyncFiles createForNotebooks(Collection<String> notebooksNames) {
    if (notebooksNames == null) {
      return new SyncFiles();
    }
    return notebooksNames.stream()
      .map(this::createForNotebook)
      .reduce(new SyncFiles(), SyncFiles::merge);

  }

  public SyncFiles createForStack(String stackname) {

    return notebookStream()
      .filter(n -> Objects.equals(stackname, n.getStack()))
      .peek(n -> LOGGER.info("Found notebook '{}' in stack '{}'", n.getName(), stackname))
      .map(this::createForNotebook)
      .reduce(new SyncFiles(), SyncFiles::merge);
  }

  public SyncFiles createForNotebook(String notebookName) {
    return notebookStream().filter(n -> Objects.equals(n.getName(), notebookName))
      .map(this::createForNotebook)
      .reduce(new SyncFiles(), SyncFiles::merge);
  }

  public SyncFiles createForNotebook(Notebook notebook) {

    final List<Note> notes = this.loadNotesIn(notebook, 0);

    if (notes.size() > 0) {
      LOGGER.debug("Found {} note[s] in notebook '{}'. Processing notes", notes.size(), notebook.getName());

      return notes.stream()
        .map(n -> this.createForNote(n, this.pathFor(notebook)))
        .reduce(new SyncFiles(), SyncFiles::merge);

    } else {
      LOGGER.debug("Notebook '{}' is empty.", notebook.getName());
    }
    return new SyncFiles();
  }

  private Stream<Notebook> notebookStream() {

    if (this.notebooks.size() == 0) {
      LOGGER.warn("Notebooks collection is empty. No notebooks found...");
    }
    return this.notebooks.stream();
  }

  private SyncFiles createForNote(Note note, SyncPath path) {

    if (note.getResources() != null && note.getResources().size() > 0) {
      final SyncFiles syncFiles = new SyncFiles();
      if (note.getResources().size() == 1) {
        LOGGER.info(
          "Note '{}' with guid {} has exactly one resources; Using note title as resources title.",
          note.getTitle(),
          note.getGuid()
        );
        note.getResources().stream()
          .map(r -> this.createForResource(r, path.append(note.getTitle() + getFileSuffix(r))))
          .peek(s -> s.withTimestamp(note.getUpdated()))
          .forEach(syncFiles::add);
      } else {
        LOGGER.info("Note '{}' with guid {} has {} resources; processing resources", note.getTitle(),
          note.getGuid(), note.getResources().size()
        );
        note.getResources().stream()
          .map(r -> this.createForResource(r, path.append(note.getTitle()).append(r.getAttributes().getFileName())))
          .peek(s -> s.withTimestamp(note.getUpdated()))
          .forEach(syncFiles::add);
      }
      return syncFiles;
    } else {
      LOGGER.info("Note '{}' with guid {} has no resources; Skipping.", note.getTitle(), note.getGuid());
    }
    return new SyncFiles();
  }

  private String getFileSuffix(Resource r) {

    final String fileName = r.getAttributes().getFileName();
    if (fileName != null) {
      final int beginIndex = fileName.lastIndexOf(".");
      if (beginIndex > 0) {
        return fileName.substring(beginIndex);
      }
    }
    return "";
  }

  public SyncFile createForResource(Resource resource, SyncPath syncPath) {

    return new SyncFile()
      .withSyncPath(syncPath)
      .withHash(resource.getData().getBodyHash())
      .withDataSupplier(() -> loadBinaryDataFrom(resource));
  }

  private byte[] loadBinaryDataFrom(Resource resource) {

    try {
      return this.noteStoreClient.getResource(resource.getGuid(), true,
          false, false, false
        )
        .getData().getBody();
    } catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
      throw new RuntimeException();
    }
  }

  private SyncPath pathFor(Notebook notebook) {
    return SyncPath.EMPTY.append(notebook.getStack()).append(notebook.getName());
  }

  private List<Note> loadNotesIn(Notebook notebook, int offset) {

    final NoteFilter noteFilter = new NoteFilter();
    noteFilter.setNotebookGuid(notebook.getGuid());
    try {
      final int maxNotes = 100;
      final NoteList notes = this.noteStoreClient.findNotes(noteFilter, offset, maxNotes);
      final ArrayList<Note> noteList = new ArrayList<>();
      if (notes.getNotesSize() > 0) {
        noteList.addAll(notes.getNotes());
      }
      if (notes.getNotesSize() >= maxNotes) {
        noteList.addAll(loadNotesIn(notebook, offset + notes.getNotesSize()));
      }
      return noteList;
    } catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
      throw new RuntimeException(
        String.format("Could not load notes for notebook '%s' with guid %s", notebook.getName(),
          notebook.getGuid()
        ), e);
    }
  }
}
