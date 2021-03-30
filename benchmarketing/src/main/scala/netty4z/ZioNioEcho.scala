package netty4z

import zio._
import zio.clock.Clock
import zio.console.Console
import zio.nio.channels._
import zio.nio.core.SocketAddress
import zio.stream.ZStream

import java.lang.{Runtime => JRuntime}

// This code doesn't work
object ZioNioEcho extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    server.exitCode
  }

  def server: ZIO[ZEnv, Exception, Unit] =
    AsynchronousServerSocketChannel()
      .use(server =>
        for {
          _ <- SocketAddress.inetSocketAddress("localhost", 8007).flatMap(server.bind(_))
          _ <- console.putStrLn("Started!")
          _ <- ZStream.repeatEffect(server.accept.preallocate)
            .map(_.withEarlyRelease)
            .forever
            .mapMPar(JRuntime.getRuntime.availableProcessors())(_.use((handleChannel _).tupled))
            .runDrain
        } yield ()
      )

  def handleChannel(closeConn: URIO[Any, Any],
                    channel: AsynchronousSocketChannel): ZIO[Clock with Console, Exception, Unit] = {
    for {
      _ <- UIO(println(channel.toString))
      _ <- ZStream
        .fromEffectOption(channel.readChunk(128).orElse(ZIO.fail(None)))
        .foreach(channel.writeChunk)
      _ <- closeConn
    } yield ()
  }
}
