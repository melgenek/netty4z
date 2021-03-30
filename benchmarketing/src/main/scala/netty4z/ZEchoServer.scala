package netty4z

import zio.{ExitCode, UIO, URIO}

object ZEchoServer extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    ZTcp.server(8007)
      .use { s =>
        UIO(println("Started!")) *>
          s.handle { ch =>
            ch.write(ch.stream)
              .catchAll {
                e => UIO(println(s"OOPS $e"))
              }

            //            ch.write(
            //              ch.stream.grouped(4)
            //                .tap(chunk => UIO(println(byteArrayToInt(chunk.toArray))))
            //                .flattenChunks
            //            ).catchAll {
            //              e => UIO(println(s"OOPS $e"))
            //            }
          }
      }
      .tap(_ => UIO(println("!!!")))
      .exitCode
  }

  def byteArrayToInt(b: Array[Byte]): Int = b(3) & 0xFF | (b(2) & 0xFF) << 8 | (b(1) & 0xFF) << 16 | (b(0) & 0xFF) << 24
}
