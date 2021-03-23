package netty4z

import io.netty.buffer.{ByteBuf, Unpooled}
import zio.Chunk

object BufferUtils {

  final def bufferToChunk(b: ByteBuf): Chunk[Byte] = {
    if (b.hasArray) Chunk.fromArray(b.array())
    else Chunk.fromByteBuffer(b.nioBuffer())
  }

  final def chunkToBuffer(chunk: Chunk[Byte]): ByteBuf = {
    Unpooled.wrappedBuffer(chunk.toArray)
  }

}
