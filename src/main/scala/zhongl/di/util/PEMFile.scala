package zhongl.di.util

import java.io.FileReader
import java.security.interfaces.RSAPrivateKey

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.{ PEMKeyPair, PEMParser }
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter

import scala.util.Try

object PEMFile {
  def asRSAPrivateKey(path: String): Try[RSAPrivateKey] = Try {
    using(new PEMParser(new FileReader(path))) { r =>
      new JcaPEMKeyConverter()
        .setProvider(new BouncyCastleProvider())
        .getKeyPair(r.readObject().asInstanceOf[PEMKeyPair])
        .getPrivate.asInstanceOf[RSAPrivateKey]
    }
  }

  def apply(path: String) = new PEMFile(path)

}

class PEMFile private (path: String) {
  def asRSAPrivateKey: Try[RSAPrivateKey] = Try {
    using(new PEMParser(new FileReader(path))) { r =>
      new JcaPEMKeyConverter()
        .setProvider(new BouncyCastleProvider())
        .getKeyPair(r.readObject().asInstanceOf[PEMKeyPair])
        .getPrivate.asInstanceOf[RSAPrivateKey]
    }
  }
}