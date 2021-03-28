package netty4z

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.stream.ChunkedInput
import netty4z.ZioChannelHandler.ChannelEnd
import zio.stream.Take
import zio.{Queue, UIO, ZQueue}

object ZQueueChunkedInput {
  def make(): UIO[(Queue[AnyRef], ZQueueChunkedInput)] = {
    for {
      q <- ZQueue.unbounded[AnyRef]
    } yield (q, new ZQueueChunkedInput(q))
  }
}

class ZQueueChunkedInput(q: Queue[AnyRef]) extends ChunkedInput[ByteBuf] {
  private var offset: Long = 0
  private var closed = false

  override def isEndOfInput: Boolean =
    closed || zRuntime.unsafeRun(q.isShutdown)

  override def close(): Unit = {
    closed = true
    zRuntime.unsafeRun(q.shutdown)
  }

  override def readChunk(ctx: ChannelHandlerContext): ByteBuf =
    readChunk(ctx.alloc())

  override def readChunk(allocator: ByteBufAllocator): ByteBuf = {
    if (isEndOfInput) null
    else {
      zRuntime.unsafeRun(q.poll).fold(null: ByteBuf) {
        case b: ByteBuf =>
          offset += b.readableBytes()
          b
        case ChannelEnd =>
          closed = true
          null
        case e: Throwable =>
          throw e
      }
    }
  }

  override def length(): Long = -1

  override def progress(): Long = offset
}
