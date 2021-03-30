package netty4z

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.stream.ChunkedWriteHandler
import zio.stream.ZStream
import zio.{Queue, Task, UIO, ZIO, ZManaged, ZQueue}

object ZTcp {

  type Handler[R, A] = ZChannel => ZIO[R, Nothing, A]

  class Server(q: Queue[ZChannel]) {
    def handle[R, A](handler: Handler[R, A]): ZIO[R, Nothing, Unit] = {
      ZStream.fromQueue(q)
        .mapMPar(Runtime.getRuntime.availableProcessors())(handler)
        .runDrain
    }
  }

  def server(port: Int): ZManaged[Any, Throwable, Server] = {
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
      _ <- (for {
        cf <- ZIO(b.bind(port))
        _ <- Task(cf).toZIO
      } yield cf).toManaged(ch => Task(ch.channel().close()).toZIO.ignore)
    } yield new Server(channelQueue)
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
          (h, zch) <- ZioChannelHandler.make(ch)
          _ <- ZIO(p.addLast(h))
          _ <- q.offer(zch)
        } yield ())
      }
    }
  }

}
