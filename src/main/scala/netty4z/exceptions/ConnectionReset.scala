package netty4z.exceptions

import netty4z.Logger

import java.io.IOException
import java.net.SocketException

object ConnectionReset {

  // https://github.com/reactor/reactor-netty/blob/85e191f8ef6fa51d5ac3dea1a266b2e7de587196/reactor-netty-core/src/main/java/reactor/netty/channel/AbortedException.java#L47
  def apply(e: Throwable): Boolean = {
    e match {
      case err: IOException if err.getMessage != null && err.getMessage.contains("Broken pipe") => true
      case err: IOException if err.getMessage != null && err.getMessage.contains("Connection reset by peer") => true
      case err: SocketException if err.getMessage != null && err.getMessage.contains("Broken pipe") => true
      case _ => false
    }
  }

  def unapply(e: Throwable): Option[Throwable] = Option.when(apply(e))(e)

}
