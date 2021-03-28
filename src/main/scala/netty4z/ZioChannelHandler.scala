package netty4z

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil
import netty4z.ZioChannelHandler.ChannelEnd
import netty4z.exceptions.ConnectionReset
import zio.{Queue, UIO, ZQueue}

object ZioChannelHandler {
  def make(ch: SocketChannel): UIO[(ZioChannelHandler, ZChannel)] = {
    for {
      in <- ZQueue.unbounded[AnyRef]
    } yield (new ZioChannelHandler(in), new ZChannel(ch, in))
  }

  case object ChannelEnd

  type ChannelEnd = ChannelEnd.type
}

class ZioChannelHandler(in: Queue[AnyRef]) extends ChannelInboundHandlerAdapter {
  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    zRuntime.unsafeRun(in.offerIfNotShutdown(ChannelEnd))
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
    if (!zRuntime.unsafeRun(in.offerIfNotShutdown(msg))) {
      ReferenceCountUtil.safeRelease(msg)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable): Unit = {
    e match {
      case ConnectionReset(_) =>
        Logger.debug(s"Connection reset.")
      case _ =>
        Logger.error(s"Unknown error: $e")
        zRuntime.unsafeRun(in.offerIfNotShutdown(e))
    }
  }
}
