package netty4z

import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import netty4z.BufferUtils.{bufferToChunk, chunkToBuffer}
import netty4z.ZioChannelHandler.ChannelEnd
import zio.stream.ZStream
import zio.{Chunk, Task, UIO, URIO, ZIO, ZRef}

import scala.util.Try

object ZChannel {

}

class ZChannel(ch: SocketChannel, in: TaskQueue[AnyRef], out: TaskQueue[ByteBuf]) {

  val stream: ZStream[Any, Throwable, Byte] = ZStream.bracket {
    for {
      _ <- Task {
        if (ch.isActive) ch.read()
      }
      buf <- in.take.catchAllCause { c =>
        in.isShutdown.flatMap { down =>
          if (down && c.interrupted) ZIO.succeed(ChannelEnd)
          else ZIO.halt(c)
        }
      }
    } yield buf
  } {
    case b: ByteBuf => URIO(b.release())
    case _ => UIO.unit
  }
    .forever
    .collectWhileM {
      case b: ByteBuf => ZIO.succeed(bufferToChunk(b))
      case e: Throwable => ZIO.fail(e)
    }
    .flattenChunks
      .ensuringFirst(
  //      printQueue *>
        in.shutdown)

  def writeChunk(chunk: Chunk[Byte]): Task[Unit] = {
    out.offer(chunkToBuffer(chunk)) *> Task {
      if (ch.isActive) ch.flush()
    }
  }

  def write(in: ZStream[Any, Throwable, Byte]): ZStream[Any, Throwable, Unit] = {
    in.mapChunks(Chunk.single)
      .mapM(chunk => writeChunk(chunk))
      .onError(c => UIO(s"ERROR ! $c"))
    //      .drain
    //      .ensuringFirst(out.shutdown)
  }

  def printQueue: UIO[Unit] = {
    in.size.map(i => println(i))
  }
}
