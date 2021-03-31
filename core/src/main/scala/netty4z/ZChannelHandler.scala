package netty4z

import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil
import netty4z.ZChannelHandler.ChannelEnd
import netty4z.exceptions.ConnectionReset
import netty4z.utils.BufferUtils.{bufferToChunk, chunkToBuffer, safeRelease}
import netty4z.utils.Logger
import zio.stream.ZStream
import zio.{Chunk, Queue, Task, UIO, ZIO, ZQueue}

object ZChannelHandler {
  def make(ch: SocketChannel): UIO[ZChannelHandler] = {
    for {
      in <- ZQueue.unbounded[AnyRef]
    } yield new ZChannelHandler(ch, in)
  }

  case object ChannelEnd

  type ChannelEnd = ChannelEnd.type
}

class ZChannelHandler(ch: SocketChannel, val in: Queue[AnyRef]) extends ChannelInboundHandlerAdapter with ZChannel {

  def buffers(): ZStream[Any, Throwable, ByteBuf] = ZStream.bracket {
    for {
      _ <- readIfActive
      buf <- in.takeIfNotShutdown(ChannelEnd)
    } yield buf
  } {
    case b: ByteBuf => safeRelease(b)
    case _ => UIO.unit
  }
    .forever
    .collectWhileM {
      case b: ByteBuf => ZIO.succeed(b)
      case e: Throwable => ZIO.fail(e)
    }
    .ensuring(in.shutdown)

  def bytes(): ZStream[Any, Throwable, Byte] =
    buffers()
      .map(bufferToChunk)
      .flattenChunks

  def writeBuffers(in: ZStream[Any, Throwable, ByteBuf]): ZIO[Any, Throwable, Unit] = {
    for {
      (q, input) <- ZQueueChunkedInput.make()
      _ <- ZIO.collectAllPar_(List(
        in
          .foreach(buffer => q.offerIfNotShutdown(buffer) *> flushIfActive)
          .ensuring(q.offerIfNotShutdown(ChannelEnd))
          .onError(e => q.offerIfNotShutdown(e.squash)),
        Task(ch.writeAndFlush(input))
      ))
    } yield ()
  }

  def writeBytes(in: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, Unit] = {
    writeBuffers(in.mapChunks(chunk => Chunk.single(chunkToBuffer(chunk))))
  }

  private val readIfActive: Task[Any] = Task(if (ch.isActive) ch.read())

  private val flushIfActive: Task[Any] = Task(if (ch.isActive) ch.flush())

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    zRuntime.unsafeRun(in.offerIfNotShutdown(ChannelEnd))
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
    if (!zRuntime.unsafeRun(in.offerIfNotShutdown(msg))) {
      ReferenceCountUtil.safeRelease(msg)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable): Unit = {
    e match {
      case ConnectionReset(_) =>
        Logger.debug(s"Connection reset.")
      case _ =>
        Logger.error(s"Unknown error", e)
        zRuntime.unsafeRun(in.offerIfNotShutdown(e))
    }
  }
}
