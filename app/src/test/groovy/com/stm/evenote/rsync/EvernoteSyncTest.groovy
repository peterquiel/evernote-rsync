package com.stm.evenote.rsync

import spock.lang.Specification

class EvernoteSyncTest extends Specification {


  void "test extract mono auth token" () {
    given:
    def evernoteSync = new EvernoteSync()

    expect:
    evernoteSync.extractMonoAuthnToken("{\n" +
      "  \"mono_authn_token\": \"S=111:U=2121:121212\",\n" +
      "  \"iss\": \"https://accounts.evernote.com\",\n" +
      "  \"exp\": 1665766073,\n" +
      "  \"iat\": 1665762473\n" +
      '}').get() == "S=111:U=2121:121212"


    and:
    evernoteSync.extractMonoAuthnToken("{\n" +
      "  \"no_token\": \"S=111:U=2121:121212\",\n" +
      "  \"iat\": 1665762473\n" +
      "}").isEmpty()

    and:
    evernoteSync.extractMonoAuthnToken("no valid").isEmpty()
  }


  void "test extract token" () {

    given:
    def jwtToken = """
      eyJraWQiOiI1Yzg4YTM2ZC04ZTE1LTQxNmQtYjg0NS00ZjBlZDY5YmYwMTIiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9
      .
      ewogICJtb25vX2F1dGhuX3Rva2VuIjogIlM9czF4eDpVPTEyMTIxOkU9YWJjOkM9Zm9vYmFyIiwK
      ICAiaXNzIjogImh0dHBzOi8vYWNjb3VudHMuZXZlcm5vdGUuY29tIiwKICAiZXhwIjogMTY2NTc2
      NjA3MywKICAiaWF0IjogMTY2NTc2MjQ3Mwp9
      """

    when:
    def token = new EvernoteSync().extractAccessTokenFrom(jwtToken)

    then:
    token.get() == "S=s1xx:U=12121:E=abc:C=foobar"
  }
}
