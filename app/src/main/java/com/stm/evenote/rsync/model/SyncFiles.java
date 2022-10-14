package com.stm.evenote.rsync.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SyncFiles {

  private final List<SyncFile> syncFiles = new ArrayList<>();

  public SyncFiles() {

  }

  public SyncFiles(Collection<SyncFile> files) {

    this.syncFiles.addAll(files);
  }

  public SyncFiles add(SyncFile syncFile) {

    this.syncFiles.add(syncFile);
    return this;
  }

  public Collection<SyncFile> getSyncFiles() {
    return Collections.unmodifiableList(this.syncFiles);
  }

  public SyncFiles merge(SyncFiles other) {

    final SyncFiles syncFiles = new SyncFiles(this.syncFiles);
    syncFiles.syncFiles.addAll(other.syncFiles);
    return syncFiles;
  }

  public List<Operation> migrateTo(SyncFiles newState, OperationFactory operationFactory) {

    final List<Operation> createOperation = newState.syncFiles.stream()
      .map(f -> migrateTo(f, operationFactory))
      .toList();

    final List<Operation> deleteOperations = this.syncFiles.stream()
      .filter(f -> newState.findByPath(f).isEmpty())
      .map(operationFactory::delete)
      .toList();

      final ArrayList<Operation> operations = new ArrayList<>(createOperation);
      operations.addAll(deleteOperations);
      return operations;
  }

  private Operation migrateTo(SyncFile newState, OperationFactory operationFactory) {

    final Optional<SyncFile> samePath = findByPath(newState);
    final SyncFiles sameHash = findAllByHash(newState);

    if (samePath.isPresent()) {
      return sameHash.findByPath(newState)
        .map(f -> operationFactory.noOp(newState))
        .orElse(operationFactory.updateFile(samePath.get(), newState));
    }
    return operationFactory.newFile(newState);
  }

  private Optional<SyncFile> findByPath(SyncFile other) {
    return this.syncFiles.stream()
      .filter(f -> f.matchesPath(other))
      .findFirst();
  }

  private SyncFiles findAllByHash(SyncFile other) {

    final List<SyncFile> collect = this.syncFiles.stream()
      .filter(f -> f.matchesHash(other))
      .collect(Collectors.toList());
    return new SyncFiles(collect);
  }

  @Override
  public String toString() {

    return this.syncFiles.stream().map(SyncFile::toString).collect(Collectors.joining("\n"));
  }
}
