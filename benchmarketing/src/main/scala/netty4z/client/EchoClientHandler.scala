package netty4z.client

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

class EchoClientHandler() extends ChannelInboundHandlerAdapter {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    for (i <- 0 until 10) {
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
    val b = msg.asInstanceOf[ByteBuf]
    while (b.isReadable()) {
      println(s"Response! ${b.readInt()}")
    }
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close
  }
}
