package netty4z

import netty4z.exceptions.ConnectionReset

object Logger {

  def error(message: String): Unit = println(s"[ERROR] $message")

  def debug(message: String): Unit = {
    println(s"[DEBUG] $message")
  }

  def info(message: String): Unit = println(s"[INFO] $message")

  def logConnectionReset(e: Throwable): Unit = {
    e match {
      case ConnectionReset(_) => Logger.debug(s"Connection reset. $e")
      case _ =>
    }
  }
}
