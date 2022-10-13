package com.stm.evenote.rsync.opfactory

import com.stm.evenote.rsync.model.SyncFile
import com.stm.evenote.rsync.model.SyncPath
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.io.FileSystemFixture

class FileSystemOperationFactoryTest extends Specification {

  @TempDir
  FileSystemFixture fsFixture

  void "test create new file and update"() {
    given:
    def syncFile = new SyncFile()
      .withDataSupplier(() -> "content".bytes)
      .withSyncPath(SyncPath.EMPTY.append("dir").append("file.txt"))

    and:
    FileSystemOperationFactory operationFactory = new FileSystemOperationFactory(fsFixture.getCurrentPath().toString(), false)

    when:
    operationFactory.newFile(syncFile).execute()

    then:
    fsFixture.file("dir/file.txt").toFile().text == "content"

    when:
    def updateSyncFile = new SyncFile()
      .withDataSupplier(() -> "new content".bytes)
      .withSyncPath(SyncPath.EMPTY.append("dir").append("file.txt"))

    and:
    operationFactory.updateFile(syncFile, updateSyncFile).execute()

    then:
    fsFixture.file("dir/file.txt").toFile().text == "new content"
  }

  void "test delete file"() {
    given:
    def file = fsFixture.file("dir/file.txt").toFile()
    def syncFile = new SyncFile()
      .withSyncPath(SyncPath.EMPTY.append("dir").append("file.txt"))

    FileSystemOperationFactory deleteOpFactory = new FileSystemOperationFactory(fsFixture.getCurrentPath().toString(), true)
    FileSystemOperationFactory deleteDisabledOpFactory = new FileSystemOperationFactory(fsFixture.getCurrentPath().toString(), false)

    when:
    file.text = "content"

    then:
    file.exists()

    when:
    deleteDisabledOpFactory.deleteFile(syncFile).execute()

    then:
    file.exists()

    when:
    deleteOpFactory.deleteFile(syncFile).execute()

    then:
    !file.exists()
  }

  void "test delete not existing file"() {
    given:
    def syncFile = new SyncFile()
      .withSyncPath(SyncPath.EMPTY.append("dir").append("file.txt"))

    FileSystemOperationFactory deleteOpFactory = new FileSystemOperationFactory(fsFixture.getCurrentPath().toString(), true)

    when:
    def file = fsFixture.file("dir/file.txt").toFile()

    then:
    !file.exists()

    when:
    deleteOpFactory.deleteFile(syncFile).execute()

    then:
    !file.exists()
  }

}
