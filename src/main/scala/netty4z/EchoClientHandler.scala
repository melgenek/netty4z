package netty4z

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

import scala.util.Random

class EchoClientHandler() extends ChannelInboundHandlerAdapter {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    for (i <- 0 until 100) {
      //      val b = new Array[Byte](100)
      //      Random.nextBytes(b)
      val message = Unpooled.buffer(4)
      message.writeInt(i)
      ctx.writeAndFlush(message)
      println(s"Sent $i")
      //      Thread.sleep(10)
    }
//    ctx.close()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    println(s"Response! ${msg.asInstanceOf[ByteBuf].readInt()}")
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close
  }
}
