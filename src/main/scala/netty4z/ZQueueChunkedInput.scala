package netty4z

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.stream.ChunkedInput

class ZQueueChunkedInput(q: TaskQueue[ByteBuf]) extends ChunkedInput[ByteBuf] {
  private var offset: Long = 0

  override def isEndOfInput: Boolean = {
    runtime.unsafeRun(q.isShutdown)
  }

  override def close(): Unit = {
    runtime.unsafeRun(q.shutdown)
  }

  override def readChunk(ctx: ChannelHandlerContext): ByteBuf = {
    readChunk(ctx.alloc())
  }

  override def readChunk(allocator: ByteBufAllocator): ByteBuf = {
    if (isEndOfInput) null
    else {
      val b = runtime.unsafeRun(q.poll)
      b.foreach(offset += _.readableBytes())
      b.orNull
    }
  }

  override def length(): Long = {
    -1
  }

  override def progress(): Long = {
    offset
  }
}
