package netty4z.utils

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.util.{IllegalReferenceCountException, ReferenceCounted}
import zio.stream.ZStream
import zio.{Chunk, UIO}

object BufferUtils {

  final def bufferToChunk(b: ByteBuf): Chunk[Byte] = {
    if (b.hasArray) Chunk.fromArray(b.array())
    else Chunk.fromByteBuffer(b.nioBuffer())
  }

  final def chunkToBuffer(chunk: Chunk[Byte]): ByteBuf = {
    Unpooled.wrappedBuffer(chunk.toArray)
  }

  final def safeRelease(buf: ReferenceCounted): UIO[Unit] = UIO {
    if (buf.refCnt() > 0) {
      try {
        buf.release()
      } catch {
        case e: IllegalReferenceCountException => Logger.debug(s"$e")
        case e => Logger.error("Unexpected error when releasing buffer", e)
      }
    }
  }

  trait Ops {

    implicit class BufferStreamOps(buffers: ZStream[Any, Throwable, ByteBuf]) {
      def retain(): ZStream[Any, Throwable, ByteBuf] = buffers.map(_.retain())
    }

  }

}
