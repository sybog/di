package zhongl.di.rest

import java.time.ZonedDateTime

trait AccessToken {
  def token: Token
  def isOutdated(expiration: ZonedDateTime = ZonedDateTime.now()): Boolean
}

object AccessToken {

  final object Outdated extends AccessToken {
    override def isOutdated(expiration: ZonedDateTime): Boolean = true

    override def token: Token = Token("outdated")
  }
}
