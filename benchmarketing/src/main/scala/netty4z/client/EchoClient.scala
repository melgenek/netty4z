package netty4z.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption}

object EchoClient {
  val HOST: String = System.getProperty("host", "127.0.0.1")
  val PORT: Int = System.getProperty("port", "8007").toInt
  val SIZE: Int = System.getProperty("size", "256").toInt

  def main(args: Array[String]): Unit = {
    val group = new NioEventLoopGroup
    try {
      val b = new Bootstrap()
      b.group(group)
        .channel(classOf[NioSocketChannel])
        .handler(new ChannelInitializer[SocketChannel]() {
          override def initChannel(ch: SocketChannel): Unit = {
            val p = ch.pipeline
            //            p.addLast(new LoggingHandler(LogLevel.INFO))
            p.addLast(new EchoClientHandler)
          }
        })
      val f = b.connect(HOST, PORT).sync
      f.channel.closeFuture.sync
    } finally {
      group.shutdownGracefully()
    }
  }
}
