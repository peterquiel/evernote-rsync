package com.stm.evenote.rsync.model

import spock.lang.Specification

class SyncFileKTest extends Specification {

  void "test"() {
    given:
    def syncFileK = new SyncFileK(SyncPath.EMPTY)

    when:
    syncFileK.timestamp = 2

    then:
    syncFileK.timestamp == 2
  }

  void "test matches hashMd5"() {
    given:
    def syncFileK = new SyncFileK(SyncPath.EMPTY)
    syncFileK.md5Hash = "12"

    def equals = new SyncFileK(SyncPath.EMPTY)
    equals.md5Hash = "12"

    def doesNotEqual = new SyncFileK(SyncPath.EMPTY)
    doesNotEqual.md5Hash = "13"

    expect:
    syncFileK.matchesHash(equals)

    and:
    !syncFileK.matchesHash(doesNotEqual)
    !syncFileK.matchesHash(new SyncFileK(SyncPath.EMPTY))
  }
}
