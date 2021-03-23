package netty4z

import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import netty4z.BufferUtils.{bufferToChunk, chunkToBuffer}
import zio.stream.ZStream
import zio.{Chunk, Queue, Task, UIO, URIO, ZIO}

object ZChannel {

}

class ZChannel(ch: SocketChannel, q: Queue[AnyRef]) {

  val stream: ZStream[Any, Throwable, Byte] = ZStream.bracket {
    for {
      _ <- Task {
        if (ch.isActive) ch.read()
      }
      buf <- q.take
    } yield buf
  } {
    case b: ByteBuf => URIO(b.release())
    case _ => UIO.unit
  }
    .forever
    .catchAllCause { c =>
      ZStream.whenCaseM(q.isShutdown) {
        case true if c.interrupted => ZStream.empty
        case _ => ZStream.halt(c)
      }
    }
    .collectWhileM {
      case b: ByteBuf => ZIO.succeed(bufferToChunk(b))
      case e: Throwable => ZIO.fail(e)
    }
    .flattenChunks
    .ensuringFirst(
      //      printQueue *>
      q.shutdown)

  def writeChunk(chunk: Chunk[Byte]): Task[Unit] = {
    Task(ch.writeAndFlush(chunkToBuffer(chunk))).toZIO.unit
  }

  def write(in: ZStream[Any, Throwable, Byte]): ZStream[Any, Throwable, Nothing] = {
    in.mapChunks(Chunk.single)
      .mapM(chunk => writeChunk(chunk))
      .drain
  }

  def printQueue: UIO[Unit] = {
    q.size.map(i => println(i))
  }
}
