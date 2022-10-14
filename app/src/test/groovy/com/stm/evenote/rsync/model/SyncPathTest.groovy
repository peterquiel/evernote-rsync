package com.stm.evenote.rsync.model

import spock.lang.Specification
import spock.lang.TempDir
import spock.util.io.FileSystemFixture

class SyncPathTest extends Specification {

  @TempDir
  FileSystemFixture fsFixture


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

  void "test is file" () {
    given:
    def dir = this.fsFixture.dir("test").toFile()
    def file = this.fsFixture.file("test/file.txt").toFile()
    file.text = "content"
    expect:
    file.isFile()
    SyncPath.EMPTY.prepend(file.absolutePath).isFile()

    and:
    dir.isDirectory()
    !SyncPath.EMPTY.prepend(dir.getAbsolutePath()).isFile()
  }

  void "test is empty dir" () {
    given:
    def empty = this.fsFixture.dir("empty dir").toFile()
    def notEmpty = this.fsFixture.dir("not empty dir").toFile()
    def file = this.fsFixture.file("not empty dir/file.txt").toFile()
    file.text = 'content'

    expect:
    empty.isDirectory()
    SyncPath.EMPTY.prepend(empty.getAbsolutePath()).isEmptyDir()

    and:
    file.isFile()
    SyncPath.EMPTY.prepend(file.getAbsolutePath()).isFile()
    ! SyncPath.EMPTY.prepend(file.getAbsolutePath()).isEmptyDir()

    and:
    notEmpty.isDirectory()
    ! SyncPath.EMPTY.prepend(notEmpty.getAbsolutePath()).isEmptyDir()
  }

}
