package netty4z

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelFuture, ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

object RawNetty {
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
          ch.config().setAutoRead(false)
          ch.pipeline().addLast(new EchoHandler)
          ch.parent().read()
          ()
        }
      })

    val cf = bootstrap.bind(8007)
    cf.sync()
    cf.channel.read()
    cf.channel().closeFuture().sync()
    ()
  }

  final class EchoHandler extends ChannelInboundHandlerAdapter {

    override def channelActive(ctx: ChannelHandlerContext) = {
      ctx.channel.read()
      ()
    }

    override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef) = {
      ctx.channel.writeAndFlush(msg).addListener { (_: ChannelFuture) =>
        ctx.channel.read()
        ()
      }
      ()
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, t: Throwable) = ()
  }

}
