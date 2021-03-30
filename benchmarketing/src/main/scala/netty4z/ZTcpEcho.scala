package netty4z

import zio.{ExitCode, UIO, URIO}

object ZTcpEcho extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    ZTcp.server(8007)
      .use { s =>
        UIO(println("Started!")) *>
          s.handle { ch =>
            ch.write(ch.stream)
              .catchAll {
                e => UIO(println(s"OOPS $e"))
              }
          }
      }
      .exitCode
  }
}
