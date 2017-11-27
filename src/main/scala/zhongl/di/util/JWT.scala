package zhongl.di.util

import java.security.interfaces.RSAPrivateKey
import java.util.Date

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.{ JWT => Auth0 }

import scala.concurrent.duration.Duration

object JWT {
  def signRSA256(iss: String, expiration: Duration, key: RSAPrivateKey): String = {
    val now = System.currentTimeMillis()
    Auth0.create()
      .withExpiresAt(new Date(expiration.toMillis + now))
      .withIssuedAt(new Date(now))
      .withIssuer(iss)
      .sign(Algorithm.RSA256(null, key))
  }
}
