package netty4z

import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import netty4z.ZioChannelHandler.ChannelEnd
import netty4z.exceptions.ConnectionReset
import zio.{Queue, ZQueue}

object ZioChannelHandler {
  def make(ch: SocketChannel) = {
    for {
      q <- ZQueue.unbounded[AnyRef]
    } yield (new ZioChannelHandler(ch, q), new ZChannel(ch, q))
  }

  case object ChannelEnd

  type ChannelEnd = ChannelEnd.type

}

class ZioChannelHandler(ch: SocketChannel, q: Queue[AnyRef]) extends ChannelInboundHandlerAdapter {

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    runtime.unsafeRun(q.offer(ChannelEnd))
    ctx.fireChannelInactive()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    runtime.unsafeRun(q.offer(msg.asInstanceOf[ByteBuf]))
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable): Unit = {
    e match {
      case ConnectionReset(_) =>
        Logger.debug(s"exceptionCaught Connection reset. $e")
      case _ =>
        Logger.error(s"Unknown error $e")
        runtime.unsafeRun(q.offer(e))
    }
  }
}
