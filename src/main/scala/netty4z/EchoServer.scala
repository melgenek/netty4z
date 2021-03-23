package netty4z

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.util.ResourceLeakDetector
import zio.UIO

object EchoServer {
  val PORT: Int = System.getProperty("port", "8007").toInt

  def main(args: Array[String]): Unit = {
    val bossGroup = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup

    try {
      val b = new ServerBootstrap
      b.group(bossGroup, workerGroup)

        .channel(classOf[NioServerSocketChannel])
        .option[java.lang.Integer](ChannelOption.SO_BACKLOG, 100)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer[SocketChannel]() {
          override def initChannel(ch: SocketChannel): Unit = {
            ch.config().setAutoRead(false)
            val p = ch.pipeline
            //p.addLast(new LoggingHandler(LogLevel.INFO));
            //            val (h, ) =  EchoServerHandler.make(ch)
            //            p.addLast(h)
            //
            //            runtime.unsafeRunAsync(h.stream.foreach {
            //              b => UIO.unit
            //            })(e => println(e))

          }
        })
      val f = b.bind(PORT).sync
      f.channel.closeFuture.sync
    } finally {
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }
}
