package netty4z

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import zio.duration._
import zio.test.{DefaultRunnableSpec, TestAspect}

trait BaseSpec extends DefaultRunnableSpec {
  override def aspects = List(TestAspect.timeout(1.minute))

  def bufWithInt(): ByteBuf = {
    val buf = ByteBufAllocator.DEFAULT.buffer(100)
    buf.writeInt(123)
    buf
  }
}
