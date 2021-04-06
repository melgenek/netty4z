package netty4z

import io.netty.buffer.ByteBufAllocator
import netty4z.ZChannelHandler.ChannelEnd
import zio.test.Assertion._
import zio.test._

object ZQueueChunkedInputSpec extends BaseSpec {
  override def spec = suite("ZQueueChunkedInput")(
    testM("should have length -1") {
      for {
        (q, input) <- ZQueueChunkedInput.make()
        _ <- q.offer(ByteBufAllocator.DEFAULT.buffer())
      } yield assert(input.length())(equalTo(-1L))
    },
    testM("should record the reading progress") {
      for {
        (q, input) <- ZQueueChunkedInput.make()
        buf = bufWithInt()
        _ <- q.offer(buf)
        result = input.readChunk(ByteBufAllocator.DEFAULT)
      } yield assert(input.progress())(equalTo(4L)) &&
        assert(result)(equalTo(buf))
    },
    testM("should not wait until a message is present in the queue") {
      for {
        (_, input) <- ZQueueChunkedInput.make()
        result = input.readChunk(ByteBufAllocator.DEFAULT)
      } yield assert(result)(isNull)
    },
    testM("should read the end of the input") {
      for {
        (q, input) <- ZQueueChunkedInput.make()
        _ <- q.offer(ChannelEnd)
        result = input.readChunk(ByteBufAllocator.DEFAULT)
        queueIsShutdown <- q.isShutdown
      } yield assert(input.isEndOfInput())(isTrue) &&
        assert(result)(isNull) &&
        assert(queueIsShutdown)(isTrue)
    },
    testM("should read nothing when the end of input is received") {
      for {
        (q, input) <- ZQueueChunkedInput.make()
        _ <- q.offer(ChannelEnd)
        _ = input.readChunk(ByteBufAllocator.DEFAULT)
        result = input.readChunk(ByteBufAllocator.DEFAULT)
      } yield assert(result)(isNull)
    },
    testM("should close the input") {
      for {
        (q, input) <- ZQueueChunkedInput.make()
        _ = input.close()
        queueIsShutdown <- q.isShutdown
      } yield assert(input.isEndOfInput())(isTrue) &&
        assert(queueIsShutdown)(isTrue)
    },
    testM("should read nothing when the input is closed") {
      for {
        (_, input) <- ZQueueChunkedInput.make()
        _ = input.close()
        result = input.readChunk(ByteBufAllocator.DEFAULT)
      } yield assert(result)(isNull)
    },
    testM("should drain the queue before the input is closed") {
      for {
        (q, input) <- ZQueueChunkedInput.make()
        buf = bufWithInt()
        _ <- q.offer(buf)
        _ = input.close()
      } yield assert(buf.refCnt())(isZero)
    }
  )

}
