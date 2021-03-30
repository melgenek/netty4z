package netty4z

import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import netty4z.BufferUtils.{bufferToChunk, chunkToBuffer}
import netty4z.ZioChannelHandler.ChannelEnd
import zio.stream.ZStream
import zio.{Queue, Task, UIO, URIO, ZIO}

class ZChannel(ch: SocketChannel, in: Queue[AnyRef]) {

  def stream: ZStream[Any, Throwable, Byte] = ZStream.bracket {
    for {
      _ <- readIfActive
      buf <- in.takeIfNotShutdown(ChannelEnd)
    } yield buf
  } {
    case b: ByteBuf => URIO(b.release())
    case _ => UIO.unit
  }
    .forever
    .collectWhileM {
      case b: ByteBuf => ZIO.succeed(b)
      case e: Throwable => ZIO.fail(e)
    }
    .map(bufferToChunk)
    .flattenChunks
    .ensuring(in.shutdown)

  def write(in: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, Unit] = {
    for {
      (q, input) <- ZQueueChunkedInput.make()
      _ <- ZIO.collectAllPar_(List(
        in
          .foreachChunk(chunk => q.offerIfNotShutdown(chunkToBuffer(chunk)) *> flushIfActive)
          .ensuring(q.offerIfNotShutdown(ChannelEnd))
          .onError(e => q.offerIfNotShutdown(e.squash)),
        Task(ch.writeAndFlush(input))
      ))
    } yield ()
  }

  private val readIfActive: Task[Any] = Task(if (ch.isActive) ch.read())

  private val flushIfActive: Task[Any] = Task(if (ch.isActive) ch.flush())
}
