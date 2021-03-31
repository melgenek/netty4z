package netty4z

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

object RawNettyEcho {
  def main(args: Array[String]): Unit = {
    val bootstrap = new ServerBootstrap
    bootstrap.group(
      new NioEventLoopGroup(1),
      new NioEventLoopGroup
    )
      .handler(new LoggingHandler(LogLevel.INFO))
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel] {
        def initChannel(ch: SocketChannel) = {
          ch.config().setAutoRead(true)
          ch.pipeline().addLast(new EchoHandler)
          ()
        }
      })

    val cf = bootstrap.bind(8007)
    println("Started!")
    cf.sync()
    cf.channel.read()
    cf.channel().closeFuture().sync()
    ()
  }

  final class EchoHandler extends ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
      ctx.writeAndFlush(msg)
    }
  }

}
