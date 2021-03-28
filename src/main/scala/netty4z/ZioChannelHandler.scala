package netty4z

import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import netty4z.ZioChannelHandler.ChannelEnd
import netty4z.exceptions.ConnectionReset
import zio.ZQueue

import scala.util.Try

object ZioChannelHandler {
  def make(ch: SocketChannel) = {
    for {
      in <- ZQueue.unbounded[AnyRef]
      out <- ZQueue.unbounded[ByteBuf]
    } yield (new ZioChannelHandler(in, out), new ZChannel(ch, in, out))
  }

  case object ChannelEnd

  type ChannelEnd = ChannelEnd.type
}

class ZioChannelHandler(in: TaskQueue[AnyRef], out: TaskQueue[ByteBuf]) extends ChannelInboundHandlerAdapter {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.write(new ZQueueChunkedInput(out))
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    runtime.unsafeRun(in.offer(ChannelEnd))
    ctx.fireChannelInactive()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    runtime.unsafeRun(in.offer(msg.asInstanceOf[ByteBuf]))
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable): Unit = {
    e match {
      case ConnectionReset(_) =>
      //        Logger.debug(s"Connection reset.")
      case _ =>
        Logger.error(s"Unknown error: $e")
        runtime.unsafeRun(in.offer(e))
    }
  }
}
