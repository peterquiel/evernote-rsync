package com.stm.evenote.rsync.model

import spock.lang.Specification

class SyncPathTest extends Specification {

  void "test append path from relative dir" () {

    when:
    def path = SyncPath.EMPTY.append("first").append("second")

    then:
    path.toString() == "first/second"
  }

  void "test prepend" () {
    when:
    def path = SyncPath.EMPTY.append("second").prepend("first")

    then:
    path.toString() == "first/second"
  }

  void "test null path" () {

    expect:
    new SyncPath(null, null).toString() == ""
    new SyncPath(new SyncPath(null, "test"), null).toString() == "test"
    new SyncPath(null, "test").toString() == "test"
  }

  void "test append segment removes starting and ending slash" (){
    expect:
    SyncPath.EMPTY.append("/test").toString() == "test"
    SyncPath.EMPTY.append("test/").toString() == "test"
    SyncPath.EMPTY.append("/test/").toString() == "test"
  }

  void "test prepend segment removes ending slash" () {
    expect:
    SyncPath.EMPTY.prepend("/test").toString() == "/test"
    SyncPath.EMPTY.prepend("test/").toString() == "test"
    SyncPath.EMPTY.prepend("/test/").toString() == "/test"
  }
}
