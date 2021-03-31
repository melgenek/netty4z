package netty4z

import io.netty.buffer.ByteBuf
import zio.ZIO
import zio.stream.ZStream

trait ZChannel {
  def buffers(): ZStream[Any, Throwable, ByteBuf]

  def bytes(): ZStream[Any, Throwable, Byte]

  def writeBuffers(in: ZStream[Any, Throwable, ByteBuf]): ZIO[Any, Throwable, Unit]

  def writeBytes(in: ZStream[Any, Throwable, Byte]): ZIO[Any, Throwable, Unit]
}
