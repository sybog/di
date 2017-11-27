package zhongl.di

package object util {
  def using[A <: java.io.Closeable, B](a: A)(f: A => B): B = {
    try {
      f(a)
    } finally {
      a.close()
    }
  }
}
