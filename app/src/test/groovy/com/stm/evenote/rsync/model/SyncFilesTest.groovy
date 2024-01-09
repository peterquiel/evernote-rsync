package com.stm.evenote.rsync.model

import spock.lang.Specification

class SyncFilesTest extends Specification {

  OperationFactory operationFactory = Mock()

  void "test merging"() {
    given:
    def one = new SyncFile()
    def two = new SyncFile()
    def three = new SyncFile()

    and:
    def filesOne = new SyncFiles([one])
    def filesTwo = new SyncFiles([two, three])

    when:
    def mergedFiles = filesOne.merge(filesTwo)

    then:
    filesOne.syncFiles == [one]
    filesTwo.syncFiles == [two, three]
    mergedFiles.syncFiles == [one, two, three]
  }

  void "test migrating"() {
    given:
    def newFile = new SyncFile().withPath("new")
      .withHash("new")

    def noOp = new SyncFile().withPath("exists")
      .withHash("exists")

    def updateTo =
      new SyncFile().withPath("update")
        .withHash("update")

    def moveTo =
      new SyncFile().withPath("move_to")
        .withHash("move")

    def newState = new SyncFiles([newFile, noOp, updateTo, moveTo])

    and:
    def moveFrom =
      new SyncFile().withPath("move_from")
        .withHash("move")

    def toDelete =
      new SyncFile().withPath("to delete")
        .withHash("to delete")

    def updateFrom = new SyncFile().withPath("update")
      .withHash("update_old_hash")

    def currentState = new SyncFiles([
      moveFrom, toDelete,
      noOp, updateFrom
    ])

    operationFactory._(_) >> {  {} as Operation }

    when:
    def operations = currentState.migrateTo(newState, this.operationFactory)

    then:
    operations.size() == 6
    1 * operationFactory.noOp(noOp) >> { {} as Operation }
    1 * operationFactory.newFile(newFile) >> { {} as Operation }
    1 * operationFactory.newFile(moveTo) >> { {} as Operation }
    1 * operationFactory.updateFile(updateFrom, updateTo) >> { {} as Operation }
    1 * operationFactory.delete(toDelete) >> { {} as Operation }
    1 * operationFactory.delete(moveFrom) >> { {} as Operation }
  }
}
