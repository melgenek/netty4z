package netty4z

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{Channel, ChannelInitializer}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.stream.ChunkedWriteHandler
import netty4z.utils.Logger
import zio.stream.ZStream
import zio.{Queue, Task, UIO, ZIO, ZManaged, ZQueue}

import java.net.InetSocketAddress

object ZTcp {

  type Handler[R, A] = ZChannel => ZIO[R, Throwable, A]

  class Server(q: Queue[ZChannel], channel: Channel) {
    val address: InetSocketAddress = channel.localAddress().asInstanceOf[InetSocketAddress]

    def handle[R, A](handler: Handler[R, A]): ZIO[R, Nothing, Unit] = {
      ZStream.fromQueue(q)
        .mapMPar(Runtime.getRuntime.availableProcessors()) { channel =>
          handler(channel).catchAll {
            e => UIO(Logger.error("There was an exception handing connection", e))
          }
        }
        .runDrain
    }
  }

  def server(): ZManaged[Any, Throwable, Server] = {
    server(0)
  }

  def server(port: Int): ZManaged[Any, Throwable, Server] = {
    server(new InetSocketAddress("127.0.0.1", port))
  }

  def server(address: InetSocketAddress): ZManaged[Any, Throwable, Server] = {
    for {
      bossGroup <- ZIO(new NioEventLoopGroup(1)).toManaged(g => Task(g.shutdownGracefully()).toZIO.ignore)
      workerGroup <- ZIO(new NioEventLoopGroup).toManaged(g => Task(g.shutdownGracefully()).toZIO.ignore)
      channelQueue <- ZQueue.unbounded[ZChannel].toManaged(_.shutdown)
      init <- initializer(channelQueue).toManaged_
      b = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(init)
      channelFuture <- (for {
        cf <- ZIO(b.bind(address))
        _ <- Task(cf).toZIO
      } yield cf).toManaged(ch => Task(ch.channel().close()).toZIO.ignore)
    } yield new Server(channelQueue, channelFuture.channel())
  }

  def initializer(q: Queue[ZChannel]): UIO[ChannelInitializer[SocketChannel]] = {
    for {
      r <- ZIO.runtime
    } yield new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit = {
        val p = ch.pipeline
        ch.config().setAutoRead(false)
        p.addLast(new ChunkedWriteHandler())

        r.unsafeRun(for {
          handler <- ZChannelHandler.make(ch)
          _ <- ZIO(p.addLast(handler))
          _ <- q.offer(handler)
        } yield ())
      }
    }
  }

}
