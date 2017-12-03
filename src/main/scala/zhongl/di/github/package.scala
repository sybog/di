package zhongl.di

import java.security.interfaces.RSAPrivateKey
import java.util.Date

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import scala.concurrent.duration._

package object github {
  type GetJWT       = () => String

  def signRSA256(iss: String, expiration: Duration, key: RSAPrivateKey): String = {
    val now = System.currentTimeMillis()
    JWT
      .create()
      .withExpiresAt(new Date(expiration.toMillis + now))
      .withIssuedAt(new Date(now))
      .withIssuer(iss)
      .sign(Algorithm.RSA256(null, key))
  }

}
